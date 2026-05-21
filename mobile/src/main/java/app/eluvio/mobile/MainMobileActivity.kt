package app.eluvio.mobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.eluvio.mobile.navigation.DiscoverRoute
import app.eluvio.mobile.navigation.MyItemsRoute
import app.eluvio.mobile.navigation.ProfileRoute
import app.eluvio.mobile.navigation.handleMobileNavEvent
import app.eluvio.mobile.navigation.installMobileGraph
import app.eluvio.mobile.theme.EluvioMobileTheme
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.Navigator
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainMobileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force dark-bar style (light icons) regardless of system light/dark mode, since the
        // app is always dark-themed. enableEdgeToEdge() with no args follows system uiMode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            EluvioMobileTheme {
                EluvioMobileApp()
            }
        }
    }
}

@Composable
private fun EluvioMobileApp() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination
    // Full-screen destinations (video player) hide the bottom nav.
    val showBottomBar = currentDestination?.hasRoute<VideoPlayerArgs>() != true

    // Single nav handler shared by all screens via LocalNavigator. VMs emit NavigationEvents
    // through `subscribeToState` and end up here. Unmapped events surface as a toast.
    val context = LocalContext.current
    val navigator: Navigator = remember(navController, context) {
        { event ->
            navController.handleMobileNavEvent(event) {
                Toast.makeText(context, "nav (unhandled): $event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) BottomNavigationBar(navController, currentDestination)
                },
                // Top inset is handled per-screen via WindowInsets.statusBars so content can
                // scroll under the status-bar scrim.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = DiscoverRoute,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    installMobileGraph()
                }
            }
            // Translucent scrim sized to the status-bar inset. Drawn on top of content so
            // the status bar stays visually distinct while items scroll underneath.
            StatusBarScrim()
        }
    }
}

@Composable
private fun StatusBarScrim() {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxWidth()
            .height(topInset)
            .background(STATUS_BAR_SCRIM_COLOR)
    )
}

private val STATUS_BAR_SCRIM_COLOR = androidx.compose.ui.graphics.Color(0x80000000)

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hasRoute(tab.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Standard bottom-nav reselect behavior: pop back to the graph's start,
                        // but save+restore each tab's state so switching preserves scroll/form.
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

private enum class BottomTab(val route: Any, val iconRes: Int, val labelRes: Int) {
    Discover(DiscoverRoute, R.drawable.ic_home, R.string.tab_discover),
    MyItems(MyItemsRoute, R.drawable.ic_bookmark, R.string.tab_my_items),
    Profile(ProfileRoute, R.drawable.ic_person, R.string.tab_profile),
}
