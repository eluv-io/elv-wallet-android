package app.eluvio.wallet.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.dashboard.discover.Discover
import app.eluvio.wallet.screens.dashboard.myitems.MyItems
import app.eluvio.wallet.screens.dashboard.profile.Profile
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.rememberToaster
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import coil.drawable.CrossfadeDrawable
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import io.reactivex.rxjava3.processors.PublishProcessor
import kotlinx.collections.immutable.ImmutableList
import java.util.concurrent.TimeUnit

@Destination<MainGraph>
@Composable
fun Dashboard() {
    hiltViewModel<DashboardViewModel>().subscribeToState { _, tabs ->
        Dashboard(tabs)
    }
}

@Composable
fun Dashboard(tabs: ImmutableList<Tabs>) {
    var selectedTab by rememberSaveable(tabs) { mutableStateOf(tabs.first()) }
    if (selectedTab !in tabs) {
        // This is a vestige of the never-used no-auth flow.
        selectedTab = tabs.first()
    }
    var backgroundImage by rememberSaveable { mutableStateOf<String?>(null) }

    AnimatedBackground(url = backgroundImage)

    val showDrawer = tabs.size > 1
    val contentFocusRequester = remember { FocusRequester() }
    ModalNavigationDrawer(
        scrimBrush = Brush.horizontalGradient(listOf(Color.Black, Color.Transparent)),
        drawerContent = { drawerValue ->
            if (showDrawer) {
                DrawerContent(
                    drawerValue,
                    tabs,
                    selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        contentFocusRequester.requestFocus()
                    },
                    onDrawerClosed = { contentFocusRequester.requestFocus() }
                )
            }
        },
        content = {
            val modifier = Modifier
                .fillMaxSize()
                .focusRequester(contentFocusRequester)
                // Drawer width is hardcoded for now, but might change in the future?
                .thenIf(showDrawer) {
                    padding(start = NavigationDrawerItemDefaults.CollapsedDrawerItemWidth)
                }
            if (LocalInspectionMode.current) {
                // Don't load real content in preview mode
                Text(
                    text = "Dashboard page content",
                    textAlign = TextAlign.Center,
                    modifier = modifier.background(Color.Red.copy(alpha = 0.5f))
                )
            } else {
                TabContent(
                    selectedTab = selectedTab,
                    onBackgroundImageSet = { backgroundImage = it },
                    modifier = modifier
                )
            }
        })
}

@Composable
private fun NavigationDrawerScope.DrawerContent(
    drawerValue: DrawerValue,
    tabs: ImmutableList<Tabs>,
    selectedTab: Tabs,
    onTabSelected: (Tabs) -> Unit,
    onDrawerClosed: () -> Unit,
) {
    val firstTabFocusRequester = remember { FocusRequester() }
    // Columns don't handle Focus well. Use LazyColumn instead.
    LazyColumn(
        Modifier
            .focusRestorer(firstTabFocusRequester)
            .thenIf(drawerValue == DrawerValue.Closed) {
                Modifier.background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.8f),
                        0.8f to Color.Black.copy(alpha = 0.3f),
                        1.0f to Color.Transparent,
                    )
                )
            }
            .fillMaxHeight()
            .padding(12.dp)
            .onKeyEvent {
                if (it.isKeyUpOf(Key.Back)) {
                    if (drawerValue == DrawerValue.Open) {
                        onDrawerClosed()
                        return@onKeyEvent true
                    }
                } else if (it.key == Key.DirectionRight) {
                    // Something consumes the UP event, so we do this on either up or down.
                    onDrawerClosed()
                    return@onKeyEvent true
                }
                false
            },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        itemsIndexed(tabs) { index, tab ->
            val selected = selectedTab == tab
            NavigationDrawerItem(
                selected = selected,
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstTabFocusRequester)
                },
                onClick = { onTabSelected(tab) },
                leadingContent = {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                },
                content = { Text(text = stringResource(tab.title)) }
            )
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Tabs,
    onBackgroundImageSet: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTab != Tabs.Discover) {
        onBackgroundImageSet(null)
    }
    AnimatedContent(
        targetState = selectedTab,
        label = "DashboardContent",
        modifier = modifier
    ) { tab ->
        when (tab) {
            Tabs.Discover -> Discover(onBackgroundImageSet = {
                if (selectedTab == Tabs.Discover) {
                    // This can get called when navigating away from Discover so we need to
                    // consider the targetState
                    onBackgroundImageSet(it)
                }
            })

            Tabs.MyItems -> MyItems()
            Tabs.Profile -> Profile()
        }
    }
}

@Composable
private fun AnimatedBackground(url: String?, modifier: Modifier = Modifier) {
    val animationDuration = CrossfadeDrawable.DEFAULT_DURATION
    AnimatedContent(
        targetState = url,
        transitionSpec = {
            // Default transition spec has a scale animation and we don't want that
            (fadeIn(animationSpec = tween(animationDuration, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        label = "bgImage"
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(it)
                .crossfade(animationDuration)
                .build(),
            contentScale = ContentScale.FillWidth,
            contentDescription = "background",
            modifier = Modifier.fillMaxSize()
        )
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

@Composable
@Preview(device = Devices.TV_720p)
private fun DashboardPreview() = EluvioThemePreview {
    Dashboard()
}
