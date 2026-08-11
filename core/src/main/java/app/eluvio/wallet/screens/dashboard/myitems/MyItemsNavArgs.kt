package app.eluvio.wallet.screens.dashboard.myitems

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Standalone destination for My Items. Normally it's reached as a Dashboard tab, but
 * single-property builds skip the Dashboard, so the Property page links here directly.
 */
@Serializable
data object MyItemsNavArgs : NavKey
