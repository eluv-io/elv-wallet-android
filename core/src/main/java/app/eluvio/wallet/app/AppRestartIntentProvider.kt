package app.eluvio.wallet.app

import android.content.Intent

/**
 * Builds an Intent that re-launches the host application from a clean back stack. Provided via
 * Hilt by the host (`:app`) module so :core code (e.g. SignOutHandler) can restart the app
 * without depending on the host's Activity types.
 */
fun interface AppRestartIntentProvider {
    fun createRestartIntent(): Intent
}
