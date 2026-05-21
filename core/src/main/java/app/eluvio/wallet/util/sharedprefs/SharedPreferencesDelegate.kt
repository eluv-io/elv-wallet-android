package app.eluvio.wallet.util.sharedprefs

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPreferencesDelegate<T>(
    private val store: SharedPreferences,
    private val key: String,
    private val defaultValue: T,
    private val getter: SharedPreferences.(String, T) -> T,
    private val setter: Editor.(String, T) -> Editor
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        store.getter(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        store.edit { setter(key, value) }
}

fun SharedPreferences.string(key: String, def: String = "") =
    SharedPreferencesDelegate(
        this, key, def,
        { k, dV -> /* SharedPreferences:: */ getString(k, null) ?: dV },
        Editor::putString
    )

fun SharedPreferences.nullableString(key: String, def: String? = null) =
    SharedPreferencesDelegate(this, key, def, SharedPreferences::getString, Editor::putString)

fun SharedPreferences.stringSet(key: String, def: Set<String> = emptySet()) =
    SharedPreferencesDelegate(
        this, key, def,
        { k, dV -> /* SharedPreferences:: */ getStringSet(k, null) ?: dV },
        Editor::putStringSet
    )

fun SharedPreferences.int(key: String, def: Int = 0) =
    SharedPreferencesDelegate(this, key, def, SharedPreferences::getInt, Editor::putInt)

fun SharedPreferences.long(key: String, def: Long = 0L) =
    SharedPreferencesDelegate(this, key, def, SharedPreferences::getLong, Editor::putLong)

fun SharedPreferences.float(key: String, def: Float = 0F) =
    SharedPreferencesDelegate(this, key, def, SharedPreferences::getFloat, Editor::putFloat)

fun SharedPreferences.boolean(key: String, def: Boolean) =
    SharedPreferencesDelegate(this, key, def, SharedPreferences::getBoolean, Editor::putBoolean)
