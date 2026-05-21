@file:Suppress("NOTHING_TO_INLINE")

package app.eluvio.wallet.util.logging

import timber.log.Timber

// Functions are inlined so that Timber sets the tag to the actual calling class.
// Without the inline, the tag is always "Log".
object Log {
    inline fun w(msg: String, throwable: Throwable? = null) = Timber.w(throwable, msg)
    inline fun d(msg: String, throwable: Throwable? = null) = Timber.d(throwable, msg)
    inline fun v(msg: String, throwable: Throwable? = null) = Timber.v(throwable, msg)
    inline fun i(msg: String, throwable: Throwable? = null) = Timber.i(throwable, msg)
    inline fun e(msg: String, throwable: Throwable? = null) = Timber.e(throwable, msg)
}
