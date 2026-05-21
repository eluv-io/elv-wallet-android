package app.eluvio.wallet.util.datastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Synchronous version of [RxDataStore.updateDataAsync].
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun RxDataStore<Preferences>.edit(action: MutablePreferences.() -> Unit) {
    updateDataAsync {
        val prefs = it.toMutablePreferences()
        prefs.action()
        Single.just(prefs)
    }
        .ignoreElement()
        // .blockingSubscribe() will not throw exceptions, so use blockingAwait() instead, to allow callers to recover.
        .blockingAwait()
}
