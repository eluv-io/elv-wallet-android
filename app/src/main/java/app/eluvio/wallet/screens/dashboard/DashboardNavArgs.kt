package app.eluvio.wallet.screens.dashboard

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The TV app's top-level dashboard. Args-less; declared in :core so :tv navigation can route to it. */
@Serializable
data object DashboardNavArgs : NavKey
