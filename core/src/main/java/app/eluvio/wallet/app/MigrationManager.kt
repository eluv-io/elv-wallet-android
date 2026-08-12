package app.eluvio.wallet.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.eluvio.wallet.util.logging.Log
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "migration_manager")
private val COMPLETED_MIGRATIONS_KEY = stringSetPreferencesKey("completed_migrations")

/**
 * One-time data migrations run at app startup. Each app provides a concrete subclass —
 * migrations are intrinsically per-app since they depend on that app's own history.
 *
 * Migrations are identified by name and recorded once seen, so each one runs at most once per
 * install. They used to be keyed on "did this install upgrade from a versionCode in range X..Y"
 * instead, which broke as soon as a whitelabel build reused :tv's migrations with its own
 * versionCode space starting at 1: a migration written for "anyone below 33" fired on every
 * single launch, forever, because the version never climbed out of the window. A name has no
 * such implicit dependency on how any given app numbers its releases.
 */
abstract class MigrationManager(context: Context) {
    private val dataStore = context.dataStore

    /**
     * Called once per app startup. Override and wrap each migration in [runOnce]. Default is a
     * no-op, for apps with nothing to migrate.
     */
    open suspend fun applyMigration() {}

    /**
     * Runs [block] if this install hasn't seen migration [id] before, and records [id] so it
     * never runs again. Ids are permanent — renaming one re-runs it on every existing install.
     *
     * [id] is recorded *before* [block] runs, not after: a migration is allowed to restart the
     * app (and is equally allowed to crash), and one that never reaches its own bookkeeping would
     * re-run on every launch — the failure this whole mechanism exists to prevent. The tradeoff
     * is that a half-finished migration is skipped rather than retried, so write them to leave
     * the app in a usable state either way.
     */
    protected suspend fun runOnce(id: String, block: suspend () -> Unit) {
        val completed = dataStore.data.first()[COMPLETED_MIGRATIONS_KEY].orEmpty()
        if (id in completed) return

        dataStore.edit { it[COMPLETED_MIGRATIONS_KEY] = completed + id }
        Log.w("Running migration: $id")
        block()
    }
}
