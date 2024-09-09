package app.eluvio.wallet.data.stores

import android.content.Context
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.util.datastore.StoreOperation
import app.eluvio.wallet.util.datastore.edit
import app.eluvio.wallet.util.datastore.readWriteStringPref
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = RxPreferenceDataStoreBuilder(context, "token_store").build()

    val idToken = dataStore.readWriteStringPref("id_token")
    val accessToken = dataStore.readWriteStringPref("access_token")
    val refreshToken = dataStore.readWriteStringPref("refresh_token")

    val clusterToken = dataStore.readWriteStringPref("cluster_token")

    val fabricToken = dataStore.readWriteStringPref("fabric_token")

    val walletAddress = dataStore.readWriteStringPref("wallet_address")

    val isLoggedIn: Boolean get() = fabricToken.get() != null
    val loggedInObservable = fabricToken.observe().map { it.isPresent }

    private val loginProviderStr = dataStore.readWriteStringPref("login_provider")
    var loginProvider: LoginProviders
        get() = LoginProviders.from(loginProviderStr.get())
        set(value) = loginProviderStr.set(value.value)

    /**
     * Update multiple preferences at once.
     * This is more performant than updating them one by one.
     */
    fun update(vararg operations: StoreOperation) = dataStore.edit {
        operations.forEach { operation ->
            operation()
        }
    }

    fun wipe() {
        dataStore.edit { clear() }
    }
}
