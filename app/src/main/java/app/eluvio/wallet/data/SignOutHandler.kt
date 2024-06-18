package app.eluvio.wallet.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.eluvio.wallet.MainActivity
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.util.compose.Toaster
import app.eluvio.wallet.util.logging.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.realm.kotlin.Realm
import javax.inject.Inject

class SignOutHandler @Inject constructor(
    private val tokenStore: TokenStore,
    private val realm: Realm,
    @ApplicationContext private val context: Context,
    private val toaster: Toaster,
) {
    fun signOut(completeMessage: String?): Completable {
        return Completable.fromAction {
            tokenStore.wipe()
            // delete entire db?
            realm.writeBlocking {
                Log.w("Deleting all realm data")
                deleteAll()
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                if (completeMessage != null) {
                    toaster.toast(completeMessage, Toast.LENGTH_LONG)
                }
                restartApp()
            }
    }

    private fun restartApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }
}
