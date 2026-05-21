package app.eluvio.wallet.data.stores

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Provides a stable unique ID for this installation of the app.
 * The ID is generated on first access and stored in a DataStore preference.
 * The ID is not tied to any user account and will remain the same across app updates,
 * but will be reset if the app is uninstalled and reinstalled.
 */
@Singleton
class Installation @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "installation_store")

    private val installationIdKey = stringPreferencesKey("installation_id")

    val id: String by lazy {
        runBlocking {
            var id = context.dataStore.data
                .map { preferences -> preferences[installationIdKey] }
                .firstOrNull()
            if (id == null) {
                id = UUID.randomUUID().toString()
                context.dataStore.edit { preferences ->
                    preferences[installationIdKey] = id
                }
            }
            return@runBlocking id
        }
    }
}
