package app.eluvio.wallet.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Toaster @Inject constructor(@ApplicationContext private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    fun toast(message: String, duration: Int = Toast.LENGTH_SHORT) = runOnUiThread {
        Toast.makeText(context, message, duration).show()
    }

    fun toast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) = runOnUiThread {
        Toast.makeText(context, message, duration).show()
    }

    private fun runOnUiThread(block: () -> Unit) {
        if (handler.looper.isCurrentThread)
            block()
        else
            handler.post(block)
    }
}
