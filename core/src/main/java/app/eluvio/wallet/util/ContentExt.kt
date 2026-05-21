package app.eluvio.wallet.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Find a [androidx.activity.ComponentActivity] from the current context.
 * By default Jetpack Compose project uses ComponentActivity for MainActivity,
 * It is a parent of [androidx.fragment.app.FragmentActivity] or [AppCompatActivity]
 */
fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
