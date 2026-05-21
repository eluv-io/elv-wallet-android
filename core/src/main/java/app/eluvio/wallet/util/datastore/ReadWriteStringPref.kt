package app.eluvio.wallet.util.datastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.rxjava3.RxDataStore
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.optionalMap
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi

fun RxDataStore<Preferences>.readWriteStringPref(
    keyName: String,
    setterTransformer: (String) -> String = { it }
) = DataStoreReadWritePref(this, stringPreferencesKey(keyName), setterTransformer)

fun RxDataStore<Preferences>.readWriteBoolPref(
    keyName: String,
    setterTransformer: (Boolean) -> Boolean = { it }
) = DataStoreReadWritePref(this, booleanPreferencesKey(keyName), setterTransformer)

interface ReadWritePref<T : Any> {
    fun observe(): Flowable<Optional<T>>
    fun get(): T?
    fun set(value: T?, editor: MutablePreferences)
    fun set(value: T?)

    /**
     * Create an operation to set the value of this preference without executing it, allowing to
     * batch multiple operations together.
     */
    infix fun to(value: T?): StoreOperation = { set(value, this) }
}

typealias StoreOperation = MutablePreferences.() -> Unit

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreReadWritePref<T : Any>(
    private val dataStore: RxDataStore<Preferences>,
    private val key: Preferences.Key<T>,
    /** Transform the value before saving it */
    private val setterTransformer: (T) -> T,
) : ReadWritePref<T> {

    override fun observe() = dataStore.data().optionalMap { it[key] }

    override fun get(): T? = observe().blockingFirst().orDefault(null)

    override fun set(value: T?, editor: MutablePreferences) {
        if (value != null) {
            editor[key] = setterTransformer(value)
        } else {
            editor.remove(key)
        }
    }

    override fun set(value: T?) {
        dataStore.edit {
            set(value, this)
        }
    }
}
