package app.eluvio.wallet.app

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * A Timber tree that logs to Firebase Crashlytics.
 * Non-error messages are recorded as log entries (visible in crash reports as breadcrumbs).
 * Errors and WTFs are recorded as non-fatal exceptions.
 */
class CrashlyticsTree : Timber.Tree() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log(message)
        if (priority >= Log.ERROR && t != null) {
            crashlytics.recordException(t)
        }
    }
}
