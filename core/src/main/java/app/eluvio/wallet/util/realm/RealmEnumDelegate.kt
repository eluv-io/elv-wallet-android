package app.eluvio.wallet.util.realm

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

/**
 * A delegate for a property that is backed by a Realm enum.
 * Using .name on enums that get persisted is a bad idea because of obfuscation, so our realm enums should have a value property.
 */
interface RealmEnum {
    val value: String
}

/**
 * Creates a delegate for an enum property that is backed by a String.
 */
inline fun <reified T> realmEnum(
    delegateProperty: KMutableProperty0<String>
): ReadWriteProperty<Any, T> where T : Enum<T>, T : RealmEnum {
    return object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return enumValues<T>().first { it.value == delegateProperty.get() }
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            delegateProperty.set(value.value)
        }
    }
}
