package app.eluvio.wallet.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "migration_manager")
private val LAST_VERSION_KEY = intPreferencesKey("last_version")

/**
 * One-time data migrations run at app startup. Each app provides a concrete subclass —
 * migrations are intrinsically per-app since they depend on that app's own version history
 * (TV's versionCode space ≠ mobile's).
 *
 * The base class handles bookkeeping: read the previously-recorded versionCode, call
 * [runMigrations] with it, and record the current versionCode for next launch. Subclasses
 * just override [runMigrations] (or don't, if they have nothing to migrate yet).
 */
abstract class MigrationManager(
    context: Context,
    private val appInfo: AppInfo,
) {
    private val dataStore = context.dataStore

    suspend fun applyMigration() {
        val lastVersionCode = dataStore.data.first()[LAST_VERSION_KEY] ?: 0
        dataStore.edit { it[LAST_VERSION_KEY] = appInfo.versionCode }
        runMigrations(lastVersionCode)
    }

    /**
     * App-specific migrations triggered by upgrades. [lastVersionCode] is the versionCode this
     * app stored at the last launch, or `0` on a fresh install. Default is a no-op.
     */
    protected open suspend fun runMigrations(lastVersionCode: Int) {}
}
