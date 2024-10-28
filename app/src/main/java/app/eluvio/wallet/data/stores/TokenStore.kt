package app.eluvio.wallet.data.stores

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.util.base58
import app.eluvio.wallet.util.datastore.ReadWritePref
import app.eluvio.wallet.util.datastore.StoreOperation
import app.eluvio.wallet.util.datastore.edit
import app.eluvio.wallet.util.datastore.readWriteStringPref
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Flowable
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface TokenStore {
    val idToken: ReadWritePref<String>
    val accessToken: ReadWritePref<String>
    val refreshToken: ReadWritePref<String>
    val clusterToken: ReadWritePref<String>
    val fabricToken: ReadWritePref<String>
    // Only really needed when showing the Purchase Gate with a Metamask login, because in any other case we have a
    // clusterToken and the expiration can be extracted from the token itself.
    val fabricTokenExpiration: ReadWritePref<String>
    val walletAddress: ReadWritePref<String>
    val userEmail: ReadWritePref<String>
    val isLoggedIn: Boolean get() = fabricToken.get() != null

    val loggedInObservable: Flowable<Boolean> get() = fabricToken.observe().map { it.isPresent }
    var loginProvider: LoginProviders

    /**
     * Update multiple preferences at once.
     * This is more performant than updating them one by one.
     */
    fun update(vararg operations: StoreOperation)

    /**
     * Clear out everything in the store.
     */
    fun wipe()
}

@Module
@InstallIn(SingletonComponent::class)
interface TokenStoreModule {
    @Singleton
    @Binds
    fun bindTokenStore(impl: PreferenceTokenStore): TokenStore
}

@Singleton
class PreferenceTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStore {
    private val dataStore = RxPreferenceDataStoreBuilder(context, "token_store").build()

    override val idToken = dataStore.readWriteStringPref("id_token")
    override val accessToken = dataStore.readWriteStringPref("access_token")
    override val refreshToken = dataStore.readWriteStringPref("refresh_token")

    override val clusterToken = dataStore.readWriteStringPref("cluster_token")

    override val fabricToken = dataStore.readWriteStringPref("fabric_token")
    override val fabricTokenExpiration = dataStore.readWriteStringPref("fabric_token_expires_at")

    override val walletAddress = dataStore.readWriteStringPref("wallet_address")
    override val userEmail = dataStore.readWriteStringPref("user_email")

    private val loginProviderStr = dataStore.readWriteStringPref("login_provider")
    override var loginProvider: LoginProviders
        get() = LoginProviders.from(loginProviderStr.get())
        set(value) = loginProviderStr.set(value.value)

    /**
     * Update multiple preferences at once.
     * This is more performant than updating them one by one.
     */
    override fun update(vararg operations: StoreOperation) = dataStore.edit {
        operations.forEach { operation ->
            operation()
        }
    }

    override fun wipe() {
        dataStore.edit { clear() }
    }
}

/**
 * Base58 encoded json representing auth token and metadata, used to pass to the web client.
 * Assumes [TokenStore.isLoggedIn] is true.
 */
fun TokenStore.encodedAuthParam(): String {
    return JSONObject().apply {
        when (val clusterToken = clusterToken.get()) {
            null -> {
                // No cluster token only applies to the Metamask case.
                put("provider", "metamask")
                put("fabricToken", fabricToken.get())
                put("expiresAt", fabricTokenExpiration.get()?.toLongOrNull())
            }

            else -> {
                put("provider", loginProvider.value)
                put("clusterToken", clusterToken)
            }
        }

        put("address", walletAddress.get())
        put("email", userEmail.get() ?: extractEmail(idToken.get()))
    }
        .toString()
        .base58
}

private fun extractEmail(idToken: String?): String? {
    idToken ?: return null
    // idToken has 3 parts. The middle part is a base64 encoded JSON that has an "email" field.
    return idToken.split(".")
        .getOrNull(1)
        .runCatching {
            Base64.decode(this, Base64.URL_SAFE)
                ?.decodeToString(throwOnInvalidSequence = true)
                ?: throw IllegalArgumentException()
        }
        .mapCatching { JSONObject(it).getString("email") }
        .getOrNull()
}
