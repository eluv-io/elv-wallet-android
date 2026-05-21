package app.eluvio.wallet.util.realm

import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.ext.toRealmDictionary
import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmList

/**
 * Same as [toRealmList], but converts [null] lists to an empty RealmList.
 */
fun <T> List<T>?.toRealmListOrEmpty(): RealmList<T> = this?.toRealmList() ?: realmListOf()

/**
 * Same as [toRealmDictionary], but converts [null] maps to an empty RealmDictionary.
 */
fun <T> Map<String, T>?.toRealmDictionaryOrEmpty(): RealmDictionary<T> =
    this?.toRealmDictionary() ?: realmDictionaryOf()
