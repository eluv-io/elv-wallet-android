package app.eluvio.wallet.data.stores

import android.content.Context
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import app.eluvio.wallet.data.entities.v2.LoginProviders
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

    val auth0Domain: ReadWritePref<String>
    val auth0ClientId: ReadWritePref<String>

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

    override val auth0Domain = dataStore.readWriteStringPref("auth0_domain")
    override val auth0ClientId = dataStore.readWriteStringPref("auth0_client_id")

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
