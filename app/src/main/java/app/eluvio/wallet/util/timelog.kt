package app.eluvio.wallet.util

import app.eluvio.wallet.util.logging.Log

object timelog {
    var lastLog = 0L
    private fun delta(): Long {
        val now = System.nanoTime() / 1000000
        val delta = now - lastLog
        lastLog = now
        return delta
    }

    fun w(msg: String, throwable: Throwable? = null) = Log.w("timelog:${delta()}ms - $msg", throwable)
    fun d(msg: String, throwable: Throwable? = null) = Log.d("timelog:${delta()}ms - $msg", throwable)
    fun v(msg: String, throwable: Throwable? = null) = Log.v("timelog:${delta()}ms - $msg", throwable)
    fun i(msg: String, throwable: Throwable? = null) = Log.i("timelog:${delta()}ms - $msg", throwable)
    fun e(msg: String, throwable: Throwable? = null) = Log.e("timelog:${delta()}ms - $msg", throwable)
}