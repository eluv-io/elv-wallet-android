package app.eluvio.wallet.data.stores

import android.content.Context
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import app.eluvio.wallet.network.api.authd.CheckTokenPayload
import app.eluvio.wallet.network.api.authd.CsatResponse
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
    val loginProvider: ReadWritePref<String>

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

/**
 * Save login information from a [CsatResponse] into the [TokenStore].
 */
fun TokenStore.login(payload: CheckTokenPayload) {
    update(
        fabricToken to payload.fabricToken,
        fabricTokenExpiration to payload.expiresAt?.toString(),
        refreshToken to payload.refreshToken,
        walletAddress to payload.address,
        clusterToken to payload.clusterToken,
        userEmail to payload.email,

        // idToken is what we get directly from Auth0 before we obtain the fabricToken from authd.
        // Once we have a fabricToken, it's no longer needed.
        idToken to null,
    )
}

fun TokenStore.login(csatResponse: CsatResponse) {
    update(
        fabricToken to csatResponse.fabricToken,
        fabricTokenExpiration to csatResponse.expiresAt?.toString(),
        refreshToken to csatResponse.refreshToken,
        walletAddress to csatResponse.address,
    )
}

/**
 * Refresh the fabric token information in the [TokenStore] from a [CsatResponse].
 */
fun TokenStore.refresh(csatResponse: CsatResponse) {
    update(
        fabricToken to csatResponse.fabricToken,
        fabricTokenExpiration to csatResponse.expiresAt?.toString(),
        refreshToken to csatResponse.refreshToken,
    )
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
    @param:ApplicationContext private val context: Context
) : TokenStore {
    private val dataStore = RxPreferenceDataStoreBuilder(context, "token_store").build()

    override val idToken = dataStore.readWriteStringPref("id_token")
    override val refreshToken = dataStore.readWriteStringPref("refresh_token")
    override val clusterToken = dataStore.readWriteStringPref("cluster_token")
    override val fabricToken = dataStore.readWriteStringPref("fabric_token")
    override val fabricTokenExpiration = dataStore.readWriteStringPref("fabric_token_expires_at")
    override val walletAddress = dataStore.readWriteStringPref("wallet_address")
    override val userEmail = dataStore.readWriteStringPref("user_email")
    override val loginProvider = dataStore.readWriteStringPref("login_provider")

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
