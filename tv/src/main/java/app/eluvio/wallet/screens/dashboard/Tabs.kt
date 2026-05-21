package app.eluvio.wallet.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import app.eluvio.wallet.R
import app.eluvio.wallet.util.compose.icons.Eluvio
import app.eluvio.wallet.util.compose.icons.MyItems

@get:StringRes
val Tabs.title: Int
    get() = when (this) {
        Tabs.Discover -> R.string.dashboard_tab_discover
        Tabs.MyItems -> R.string.dashboard_tab_my_items
        Tabs.Profile -> R.string.dashboard_tab_profile
    }

val Tabs.icon: ImageVector
    get() = when (this) {
        Tabs.Discover -> Icons.Default.Home
        Tabs.MyItems -> Icons.Eluvio.MyItems
        Tabs.Profile -> Icons.Default.AccountCircle
    }
