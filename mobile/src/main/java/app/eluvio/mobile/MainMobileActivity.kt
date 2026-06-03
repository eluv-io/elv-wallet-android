package app.eluvio.mobile

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import app.eluvio.mobile.navigation.DiscoverRoute
import app.eluvio.mobile.navigation.FadeDialogSceneStrategy
import app.eluvio.mobile.navigation.MobileNavigationState
import app.eluvio.mobile.navigation.MyItemsRoute
import app.eluvio.mobile.navigation.ProfileRoute
import app.eluvio.mobile.navigation.handleMobileNavEvent
import app.eluvio.mobile.navigation.mobileEntryProvider
import app.eluvio.mobile.navigation.rememberMobileNavigationState
import app.eluvio.mobile.navigation.toEntries
import app.eluvio.mobile.theme.EluvioMobileTheme
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.Navigator
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
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
    val navState = rememberMobileNavigationState(
        startRoute = DiscoverRoute,
        topLevelRoutes = setOf(DiscoverRoute, MyItemsRoute, ProfileRoute),
    )
    val entryProvider = remember { mobileEntryProvider() }
    val entries = navState.toEntries(entryProvider)

    // Top of the active tab's stack drives whether full-screen routes hide the bottom bar.
    val activeTop = navState.backStacks[navState.topLevelRoute]?.lastOrNull()
    val showBottomBar = activeTop !is VideoPlayerArgs

    // Single nav handler shared by all screens via LocalNavigator. VMs emit NavigationEvents
    // through `subscribeToState` and end up here. Unmapped events surface as a toast.
    val context = LocalContext.current
    val navigator: Navigator = remember(navState, context) {
        { event ->
            navState.handleMobileNavEvent(event) {
                Toast.makeText(context, "nav (unhandled): $event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // FadeDialogSceneStrategy renders entries marked with dialog metadata (e.g. the translucent
    // sign-in overlay) as a Dialog on top of the previous entry, cross-faded via the overlay
    // scene's onRemove hook.
    val sceneStrategies = remember { listOf(FadeDialogSceneStrategy<NavKey>()) }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) BottomNavigationBar(navState)
                },
                // Top inset is handled per-screen via WindowInsets.statusBars so content can
                // scroll under the status-bar scrim.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                NavDisplay(
                    entries = entries,
                    sceneStrategies = sceneStrategies,
                    onBack = { navState.handleMobileNavEvent(NavigationEvent.GoBack) {} },
                    modifier = Modifier.padding(innerPadding),
                )
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
private fun BottomNavigationBar(navState: MobileNavigationState) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = navState.topLevelRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        // Tab reselect: pop this tab back to its root so the user can "scroll
                        // back to the top of the tab" by tapping it again, matching Nav 2's
                        // launchSingleTop + restoreState dance.
                        val stack = navState.backStacks[tab.route] ?: return@NavigationBarItem
                        while (stack.size > 1) stack.removeAt(stack.lastIndex)
                    } else {
                        navState.topLevelRoute = tab.route
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

private enum class BottomTab(
    val route: androidx.navigation3.runtime.NavKey,
    val iconRes: Int,
    val labelRes: Int,
) {
    Discover(DiscoverRoute, R.drawable.ic_home, R.string.tab_discover),
    MyItems(MyItemsRoute, R.drawable.ic_bookmark, R.string.tab_my_items),
    Profile(ProfileRoute, R.drawable.ic_person, R.string.tab_profile),
}
