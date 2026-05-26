package app.eluvio.mobile.screens.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.eluvio.mobile.BuildConfig

/**
 * Counts taps within a sliding [windowMs] window. Returns `true` once [threshold] consecutive
 * taps have been recorded — at which point the counter resets.
 */
class TapCounter(private val threshold: Int = 7, private val windowMs: Long = 2_000L) {
    private var count = 0
    private var lastTapAt = 0L

    fun tap(): Boolean {
        val now = SystemClock.uptimeMillis()
        count = if (now - lastTapAt > windowMs) 1 else count + 1
        lastTapAt = now
        if (count >= threshold) {
            count = 0
            return true
        }
        return false
    }
}

/** Remembers a [TapCounter] across recompositions of the profile screen. */
@Composable
fun rememberNetworkRowTapper(): TapCounter = remember { TapCounter() }

/**
 * Launches the debug env-switcher activity (only in DEBUG builds — silent in release).
 * Lives in :core debug source set under the fully-qualified name resolved by ComponentName.
 */
fun Context.launchEnvSwitcherIfDebug() {
    if (!BuildConfig.DEBUG) return
    startActivity(Intent().apply {
        component = ComponentName(packageName, "app.eluvio.wallet.debug.EnvSelectActivity")
    })
}
