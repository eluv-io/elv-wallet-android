package app.eluvio.wallet.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.NavGraphs
import app.eluvio.wallet.screens.common.AppLogo
import app.eluvio.wallet.screens.common.EluvioTab
import app.eluvio.wallet.screens.common.EluvioTabIndicator
import app.eluvio.wallet.screens.common.FocusGroup
import app.eluvio.wallet.screens.common.FocusGroupScope
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.requestInitialFocus
import app.eluvio.wallet.theme.header_30
import app.eluvio.wallet.util.compose.rememberToaster
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import io.reactivex.rxjava3.processors.PublishProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@MainGraph(start = true)
@Destination
@Composable
fun Dashboard() {
    val tabNavController = rememberNavController()
    tabNavController.addOnDestinationChangedListener { _, destination, _ ->
        Log.e("Tab destination changed: $destination")
    }
    val selectedTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val tabFocusRequesters = remember { List(Tabs.entries.size) { FocusRequester() } }
    var topBarFocused by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current

    // This is a temp semi-fix to focus problems when navigating back to dashboard. This will just
    // focus the tabs again. I still couldn't find a good way to focus on the tab content itself, if
    // it was focused when we left.
    val dashboardFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var refocusJob by remember { mutableStateOf<Job?>(null) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .focusRequester(dashboardFocusRequester)
            .onFocusChanged {
                // Focus will oscillate between Active and Inactive quickly while changing focus
                // between the Tab Row and the content, but never stay on Inactive for too long.
                // However, when coming back from another screen, it'll settle on Inactive, which
                // should never really happen, because this Column contains the entire screen, so
                // *something* inside it needs to be focused. So we'll wait 10ms and force focus
                // back if we lost it.
                refocusJob?.cancel()
                refocusJob = scope.launch {
                    delay(10)
                    // Still no focus after delay - we truly lost it
                    if (!it.hasFocus) {
                        dashboardFocusRequester.requestFocus()
                    }
                    refocusJob = null
                }
            }
            .onKeyEvent {
                // Capture back presses
                if (it.isKeyUpOf(Key.Back)) {
                    if (!topBarFocused) {
                        tabFocusRequesters[selectedTabIndex.intValue].requestFocus()
                    } else if (selectedTabIndex.intValue != 0) {
                        tabFocusRequesters[0].requestFocus()
                    } else {
                        navigator(NavigationEvent.GoBack)
                    }
                    return@onKeyEvent true
                }
                false
            }
    ) {
        TopBar(
            selectedTabIndex,
            tabFocusRequesters,
            onTabSelected = { tab, index ->
                selectedTabIndex.intValue = index
                tabNavController.navigate(tab.direction) {
                    popUpTo(tabNavController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .onFocusChanged {
                    topBarFocused = it.hasFocus
                }
        )
        val modifier = Modifier.fillMaxSize()
        if (LocalInspectionMode.current) {
            // Don't load real content in preview mode
            Text(
                text = "Dashboard page content",
                textAlign = TextAlign.Center,
                modifier = modifier.background(Color.Red.copy(alpha = 0.5f))
            )
        } else {
            DestinationsNavHost(
                navGraph = NavGraphs.dashboardTabsGraph,
                navController = tabNavController,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
private fun TopBar(
    // passing as State to delay read as much as possible
    selectedTabIndex: IntState,
    tabFocusRequesters: List<FocusRequester>,
    onTabSelected: (Tabs, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: there's nothing stopping the logo and tabs from overlapping if the screen isn't wide enough
    FocusGroup(contentAlignment = Alignment.Center, modifier = modifier) {
        val focusManager = LocalFocusManager.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Overscan.defaultPadding(excludeBottom = true))
        ) {
            AppLogo(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xff373737))
                    .padding(5.dp)
                    .align(Alignment.CenterVertically)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex.intValue,
                    contentColor = Color.White,
                    indicator = { tabPositions, doesTabRowHaveFocus ->
                        EluvioTabIndicator(
                            selectedTabIndex.intValue,
                            tabPositions,
                            doesTabRowHaveFocus
                        )
                    },
                    modifier = Modifier.requestInitialFocus()
                ) {
                    Tabs.entries.forEachIndexed { index, tab ->
                        val selected by remember {
                            derivedStateOf { selectedTabIndex.intValue == index }
                        }
                        DashboardTab(
                            tab,
                            tabRowScope = this,
                            selected = selected,
                            onFocus = {
                                if (!selected) {
                                    onTabSelected(tab, index)
                                    Log.v("Tab focused: $tab")
                                }
                            },
                            onClick = { focusManager.moveFocus(FocusDirection.Down) },
                            modifier = Modifier.focusRequester(tabFocusRequesters[index])
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
private fun FocusGroupScope.DashboardTab(
    tab: Tabs,
    tabRowScope: TabRowScope,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subject = remember { PublishProcessor.create<Any>() }
    PrintVersionOnMultiClick(subject)

    tabRowScope.EluvioTab(
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .onKeyEvent {
                if (tab == Tabs.Profile && it.isKeyUpOf(Key.DirectionRight)) {
                    subject.onNext(true)
                }
                false
            }
            .restorableFocus(),
    ) {
        val icon = tab.icon
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(tab.title),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically)
            )
        } else {
            Text(
                text = stringResource(tab.title),
                // TODO: use standard font
                style = MaterialTheme.typography.header_30.copy(fontWeight = FontWeight.W600),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun PrintVersionOnMultiClick(subject: PublishProcessor<Any>, clickThreshold: Int = 4) {
    val tripleClick by subject.buffer(3, TimeUnit.SECONDS, clickThreshold)
        .map { it.size >= clickThreshold }
        .distinctUntilChanged()
        .subscribeAsState(initial = false)
    if (tripleClick) {
        rememberToaster().toast(
            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
    }
}

//@DashboardTabsGraph
//@Destination
//@Composable
//fun Search() {
//    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
//        Text(text = "Welcome to ${stringResource(id = Tabs.Search.title)}")
//    }
//}

@Composable
@Preview(widthDp = 900)
private fun TopBarPreview() {
    TopBar(
        selectedTabIndex = remember { mutableIntStateOf(0) },
        tabFocusRequesters = remember { List(Tabs.entries.size) { FocusRequester() } },
        onTabSelected = { _, _ -> }
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun DashboardPreview() {
    Dashboard()
}
