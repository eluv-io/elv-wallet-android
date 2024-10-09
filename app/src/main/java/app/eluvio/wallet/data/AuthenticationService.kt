package app.eluvio.wallet.data

import android.util.Base64
import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.AuthServicesApi
import app.eluvio.wallet.network.api.authd.SignBody
import app.eluvio.wallet.util.crypto.Base58
import app.eluvio.wallet.util.crypto.Keccak
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.toHexByteArray
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Single
import java.util.Date
import java.util.zip.Deflater
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

interface AuthenticationService {
    /**
     * Generate a new fabric token and store it in the [TokenStore].
     * Assumes clusterToken and idToken are already present in the [TokenStore].
     */
    fun getFabricToken(): Single<String>
    fun getFabricTokenExternal(): Single<String>
}

@Module
@InstallIn(SingletonComponent::class)
interface AuthServiceModule {
    @Singleton
    @Binds
    fun bindAuthService(impl: AuthenticationServiceImpl): AuthenticationService
}

@Singleton
class AuthenticationServiceImpl @Inject constructor(
    private val apiProvider: ApiProvider,
    private val fabricConfigStore: FabricConfigStore,
    private val tokenStore: TokenStore,
) : AuthenticationService {
    override fun getFabricToken(): Single<String> {
        return apiProvider.getApi(AuthServicesApi::class).flatMap { api -> getFabricToken(api) }
    }

    override fun getFabricTokenExternal(): Single<String> {
        return apiProvider.getExternalWalletApi(AuthServicesApi::class)
            .flatMap { api -> getFabricToken(api) }
    }

    private fun getFabricToken(authServicesApi: AuthServicesApi): Single<String> {
        return fabricConfigStore.observeFabricConfiguration()
            .firstOrError()
            .flatMap { fabricConfig ->
                authServicesApi.authdLogin()
                    .doOnSuccess {
                        Log.d("login response: $it")
                        tokenStore.update(
                            tokenStore.clusterToken to it.token,
                            tokenStore.walletAddress to it.address
                        )
                    }
                    .flatMap { jwtResponse ->
                        val (accountId, hash, tokenString, expiresAt) = createTokenParts(
                            jwtResponse.address,
                            fabricConfig.qspace.id
                        )
                        remoteSign(hash, accountId, authServicesApi)
                            .map { signature ->
                                createFabricToken(tokenString, signature).also {
                                    tokenStore.update(
                                        tokenStore.fabricToken to it,
                                        tokenStore.fabricTokenExpiration to expiresAt.toString()
                                    )
                                }
                            }
                    }
            }
    }

    private fun createFabricToken(tokenString: String, signature: String): String {
        val compressedToken = tokenString.zlibCompress()
        val bytes = signature.toHexByteArray() + compressedToken
        return "acspjc${Base58.encode(bytes)}".also {
            Log.d("fabric token: $it")
        }
    }

    private fun createTokenParts(address: String, qspace: String): TokenParts {
        val addressBytes = address.toHexByteArray()
        val base64Address = Base64.encodeToString(addressBytes, Base64.DEFAULT)
        val base58Address = Base58.encode(addressBytes)
        val sub = "iusr${base58Address}"
        val expiresAt = Date().time + 24.hours.inWholeMilliseconds
        val accountId = "ikms${base58Address}"

        val tokenString = """
            {
            "sub": "$sub",
            "adr": "$base64Address",
            "spc": "$qspace",
            "iat": ${Date().time},
            "exp": $expiresAt
            }
        """.replace(Regex("\\n|\\s"), "")
        Log.d("tokenString before signing: $tokenString")

        val hash = keccak256("Eluvio Content Fabric Access Token 1.0\n$tokenString").toHexString()
        Log.d("eth msg hash: $hash")
        return TokenParts(accountId, hash, tokenString, expiresAt)
    }

    data class TokenParts(val accountId: String, val hash: String, val tokenString: String, val expiresAt: Long)

    /**
     * [accountId] is a base58 encoded string of the address prefixed with "ikms"
     */
    private fun remoteSign(
        hash: String,
        accountId: String,
        authServicesApi: AuthServicesApi
    ): Single<String> {
        return authServicesApi.authdSign(accountId, SignBody(hash))
            .map { it.signature }
            .doOnSuccess { Log.d("Signature obtained: $it") }
    }

    private fun keccak256(message: String): ByteArray {
        return Keccak(256).apply {
            update("\u0019Ethereum Signed Message:\n${message.length}$message".toByteArray())
        }.digestArray()
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun String.zlibCompress(): ByteArray {
        val input = this.toByteArray()
        val output = ByteArray(input.size * 4)
        val noWrap = true // This is *EXTREMELY* important, otherwise you'll get the wrong results
        val compressor = Deflater(Deflater.DEFAULT_COMPRESSION, noWrap).apply {
            setInput(input)
            finish()
        }
        val compressedDataLength = compressor.deflate(output)
        compressor.end()
        return output.copyOfRange(0, compressedDataLength)
    }
}
