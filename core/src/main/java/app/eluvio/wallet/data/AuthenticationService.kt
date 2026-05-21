package app.eluvio.wallet.data

import app.eluvio.wallet.data.stores.Installation
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.data.stores.login
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.CsatRequestBody
import app.eluvio.wallet.network.api.authd.DeepLinkAuthApi
import app.eluvio.wallet.util.logging.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Only used from an outdated deeplink scenario.
 */
interface AuthenticationService {
    fun getFabricTokenExternal(tenantId: String?): Single<String>
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
    private val tokenStore: TokenStore,
    private val installation: Installation,
) : AuthenticationService {

    override fun getFabricTokenExternal(tenantId: String?): Single<String> {
        return apiProvider.getExternalWalletApi(DeepLinkAuthApi::class)
            .flatMap { api -> getFabricToken(api, tenantId) }
    }

    private fun getFabricToken(
        authServicesApi: DeepLinkAuthApi,
        tenantId: String?
    ): Single<String> {
        return authServicesApi.authdLogin()
            .doOnSuccess {
                Log.d("login response: $it")
                tokenStore.update(
                    tokenStore.clusterToken to it.token,
                    tokenStore.walletAddress to it.address
                )
            }
            .flatMap {
                authServicesApi.csat(
                    CsatRequestBody(
                        tenantId,
                        nonce = installation.id,
                        email = tokenStore.userEmail.get(),
                    )
                )
            }
            .doOnSuccess { tokenStore.login(it) }
            .map { it.fabricToken }
    }
}
