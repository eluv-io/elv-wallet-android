package app.eluvio.wallet.data

import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.eluvio.wallet.MainActivity
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.util.Toaster
import app.eluvio.wallet.util.logging.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.realm.kotlin.Realm
import javax.inject.Inject

interface SignOutHandler {
    /**
     * Clears local cache and tokens. Optionally shows a message and restarts the app.
     */
    fun signOut(
        completeMessage: String? = null,
        restartAppOnComplete: Boolean = true
    ): Completable
}

@Module
@InstallIn(SingletonComponent::class)
interface SignOutHandlerModule {
    @Binds
    fun bindSignOutHandler(impl: SignOutHandlerImpl): SignOutHandler
}

class SignOutHandlerImpl @Inject constructor(
    private val tokenStore: TokenStore,
    private val realm: Realm,
    private val playbackStore: PlaybackStore,
    @param:ApplicationContext private val context: Context,
    private val toaster: Toaster,
) : SignOutHandler {
    override fun signOut(
        completeMessage: String?,
        restartAppOnComplete: Boolean
    ): Completable {
        return Completable.fromAction {
            playbackStore.wipe()
            tokenStore.wipe()
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
                if (restartAppOnComplete) {
                    restartApp()
                }
            }
    }

    private fun restartApp() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }
}
