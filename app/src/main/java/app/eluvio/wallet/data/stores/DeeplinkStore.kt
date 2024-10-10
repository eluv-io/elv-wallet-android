package app.eluvio.wallet.data.stores

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.CheckResult
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveAsync
import app.eluvio.wallet.util.rx.doOnSuccessAsync
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.sharedprefs.boolean
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject

class DeeplinkStore @Inject constructor(
    @ApplicationContext context: Context,
    private val realm: Realm,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("deeplink_store", Context.MODE_PRIVATE)

    /**
     * Whether or not the install referrer data has been handled.
     */
    var installRefHandled by prefs.boolean("installRefHandled", false)

    /**
     * Returns a deeplink from DB, if any. If it exists, it will be removed from the DB.
     */
    fun consumeDeeplinkRequest(): Maybe<DeeplinkRequestEntity> {
        return realm.query<DeeplinkRequestEntity>()
            .asFlowable()
            .firstElement()
            .mapNotNull { it.firstOrNull() }
            .doOnSuccessAsync {
                Log.d("Deeplink found in DB, passing along and removing from db")
                realm.saveAsync(emptyList<DeeplinkRequestEntity>(), clearTable = true)
            }
    }

    @CheckResult("Subscribe to the returned Completable to perform the operation")
    fun setDeeplinkRequest(request: DeeplinkRequestEntity): Completable {
        return realm.saveAsync(listOf(request), clearTable = true)
    }
}
