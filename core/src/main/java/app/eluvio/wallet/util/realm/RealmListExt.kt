package app.eluvio.wallet.util.realm

import io.github.xilinjia.krdb.ext.realmDictionaryOf
import io.github.xilinjia.krdb.ext.realmListOf
import io.github.xilinjia.krdb.ext.toRealmDictionary
import io.github.xilinjia.krdb.ext.toRealmList
import io.github.xilinjia.krdb.types.RealmDictionary
import io.github.xilinjia.krdb.types.RealmList

/**
 * Same as [toRealmList], but converts [null] lists to an empty RealmList.
 */
fun <T> List<T>?.toRealmListOrEmpty(): RealmList<T> = this?.toRealmList() ?: realmListOf()

/**
 * Same as [toRealmDictionary], but converts [null] maps to an empty RealmDictionary.
 */
fun <T> Map<String, T>?.toRealmDictionaryOrEmpty(): RealmDictionary<T> =
    this?.toRealmDictionary() ?: realmDictionaryOf()
