package app.eluvio.wallet.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.await
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "migration_manager")

class MigrationManager @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenStore: TokenStore,
    private val signOutHandler: SignOutHandler,
) {
    private val lastVersion = intPreferencesKey("last_version")
    private val dataStore = context.dataStore

    suspend fun applyMigration() {
        val lastVersionCode = dataStore.data.first()[lastVersion] ?: 0
        dataStore.edit { it[lastVersion] = BuildConfig.VERSION_CODE }

        if (lastVersionCode <= 33 && tokenStore.isLoggedIn) {
            // User is logged in since before we had refresh_tokens. Their session won't last long
            // anyway, might as well get it over now and not worry backwards compatibility of
            // "login_provider" changes in TokenStore.
            signOutHandler.signOut(completeMessage = null, restartAppOnComplete = true).await()
        }
    }
}
