package app.eluvio.wallet.screens.dashboard

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerItemShape
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.R
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.screens.dashboard.discover.Discover
import app.eluvio.wallet.screens.dashboard.myitems.MyItems
import app.eluvio.wallet.screens.dashboard.profile.Profile
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rememberToaster
import app.eluvio.wallet.util.subscribeToState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.reactivex.rxjava3.processors.PublishProcessor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

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
    // Not rememberSaveable: tabs (re)set the background on composition, so it survives
    // recreation without a custom Saver.
    var background by remember { mutableStateOf<DashboardBackground?>(null) }

    AnimatedBackground(background)

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
                    onBackgroundSet = { background = it },
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
    val railPadding = 12.dp
    val collapsedRailWidth =
        railPadding * 2 + NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
    // Hiding and showing key off different signals. drawerValue flips the moment the animation
    // starts, which is what we want on open — the divider leaves at once, ahead of the items
    // expanding over it. On close that's too early: the items are still sliding back in and the
    // divider would land on top of them. They animate their own width (see NavigationDrawerItem),
    // so the rail measuring back down to its collapsed width is what marks the collapse as done.
    val collapsedRailWidthPx = with(LocalDensity.current) { collapsedRailWidth.roundToPx() }
    var railCollapsed by remember { mutableStateOf(true) }
    val showDivider = railCollapsed && drawerValue == DrawerValue.Closed
    // Not `by`: read inside drawBehind so each animation frame only invalidates draw.
    val dividerAlpha = animateFloatAsState(
        targetValue = if (showDivider) 1f else 0f,
        // Fades in, but cuts out — see above.
        animationSpec = if (showDivider) tween(durationMillis = 300) else snap(),
        label = "navDividerAlpha"
    )
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
            .onSizeChanged { railCollapsed = it.width <= collapsedRailWidthPx }
            // Hairline at the rail's outer edge: padding + item + padding, so it clears the
            // selected item's highlight by the same 12dp that insets it from the screen edge.
            // Not the drawer width the tab content is inset by — that one lands 12dp short and
            // cuts straight through the highlight.
            // Per the design it doesn't run edge to edge: it fades up from nothing, peaks just
            // past center, and is gone before the bottom.
            .drawBehind {
                val alpha = dividerAlpha.value
                if (alpha == 0f) return@drawBehind
                val x = collapsedRailWidth.toPx()
                drawLine(
                    brush = Brush.verticalGradient(
                        0.10f to Color.Transparent,
                        0.55f to Color.White.copy(alpha = 0.45f),
                        0.93f to Color.Transparent,
                    ),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                    alpha = alpha,
                )
            }
            .fillMaxHeight()
            .padding(railPadding)
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
            NavRailItem(
                selected = selected,
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstTabFocusRequester)
                },
                onClick = { onTabSelected(tab) },
                shape = NavigationDrawerItemDefaults.shape(NavDrawerItemShape),
                leadingContent = {
                    // Sized to the width NavigationDrawerItem gives its leading slot: the tab
                    // icons aren't all 24dp squares (MyItems is a 39x49 vector), and an Icon
                    // left to its painter's intrinsic size renders narrow and top-start aligned
                    // in that slot. Any value >= the slot works, since the slot's constraints
                    // clamp it — but naming it beats a magic number that only happens to.
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                    )
                },
                content = { Text(text = stringResource(tab.title)) }
            )
        }
    }
}

/**
 * How wide a rail item gets once the drawer has focus. [NavigationDrawerItem]'s own 256dp leaves
 * a lot of empty pill trailing the label.
 */
private val ExpandedNavRailItemWidth = 180.dp

/**
 * Stands in for [NavigationDrawerItem], which hardcodes its expanded width to
 * [NavigationDrawerItemDefaults.ExpandedDrawerItemWidth] with no way to override it — it forces
 * the width from inside a layout modifier, so constraining it from the outside doesn't take.
 *
 * This is the same composition it builds — a toggleable [ListItem] whose label animates in with
 * the drawer — differing only in the width it animates to. Shape and colors are mapped across
 * exactly as [NavigationDrawerItem] maps them, so the rail still picks up the theme (including
 * the dimmed content color for when the drawer is unfocused) rather than hardcoding its own.
 *
 * Signature-compatible with [NavigationDrawerItem] for the parameters we use, so it stays a
 * drop-in either way — swapping back is a one-word change if the width ever becomes settable.
 */
@Composable
private fun NavigationDrawerScope.NavRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    shape: NavigationDrawerItemShape,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val width by animateDpAsState(
        targetValue = if (hasFocus) {
            ExpandedNavRailItemWidth
        } else {
            NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
        },
        label = "navRailItemWidth"
    )
    val colors = NavigationDrawerItemDefaults.colors()
    ListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit,
            ) {
                content()
            }
        },
        leadingContent = {
            Box(Modifier.size(NavigationDrawerItemDefaults.IconSize)) { leadingContent() }
        },
        modifier = modifier.layout { measurable, constraints ->
            val itemWidth = width.roundToPx()
            val itemHeight = NavigationDrawerItemDefaults.ContainerHeightOneLine.roundToPx()
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = itemWidth,
                    maxWidth = itemWidth,
                    minHeight = itemHeight,
                    maxHeight = itemHeight,
                )
            )
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        },
        shape = ListItemDefaults.shape(
            shape = shape.shape,
            focusedShape = shape.focusedShape,
            pressedShape = shape.pressedShape,
            selectedShape = shape.selectedShape,
            disabledShape = shape.disabledShape,
            focusedSelectedShape = shape.focusedSelectedShape,
            focusedDisabledShape = shape.focusedDisabledShape,
            pressedSelectedShape = shape.pressedSelectedShape,
        ),
        colors = ListItemDefaults.colors(
            containerColor = colors.containerColor,
            contentColor = if (hasFocus) colors.contentColor else colors.inactiveContentColor,
            focusedContainerColor = colors.focusedContainerColor,
            focusedContentColor = colors.focusedContentColor,
            pressedContainerColor = colors.pressedContainerColor,
            pressedContentColor = colors.pressedContentColor,
            selectedContainerColor = colors.selectedContainerColor,
            selectedContentColor = colors.selectedContentColor,
            disabledContainerColor = colors.disabledContainerColor,
            disabledContentColor = if (hasFocus) {
                colors.disabledContentColor
            } else {
                colors.disabledInactiveContentColor
            },
            focusedSelectedContainerColor = colors.focusedSelectedContainerColor,
            focusedSelectedContentColor = colors.focusedSelectedContentColor,
            pressedSelectedContainerColor = colors.pressedSelectedContainerColor,
            pressedSelectedContentColor = colors.pressedSelectedContentColor,
        ),
        // NavigationDrawerItem passes Scale.None; ListItem's own default grows on focus.
        scale = ListItemDefaults.scale(focusedScale = 1f),
    )
}

/**
 * The collapsed drawer item is wider than it is tall (56x48), so the default 50%-rounded shape
 * leaves a straight section between the end caps — a stumpy pill rather than a circle. Insetting
 * each side by half that difference squares the collapsed item off into a true circle.
 *
 * Derived from the container size rather than the drawer's open/closed state on purpose: the
 * state flips when the open/close animation starts, so keying off it would snap the highlight to
 * a circle while the item is still full width. As a function of size it just morphs along with
 * the item — at the cost of the expanded pill also being inset, by the same 4dp.
 */
private val NavDrawerItemShape = object : Shape {
    private val horizontalInset =
        (NavigationDrawerItemDefaults.CollapsedDrawerItemWidth -
            NavigationDrawerItemDefaults.ContainerHeightOneLine) / 2

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val inset = with(density) { horizontalInset.toPx() }
        return Outline.Rounded(
            RoundRect(
                Rect(inset, 0f, size.width - inset, size.height),
                CornerRadius(size.height / 2f)
            )
        )
    }
}

/**
 * A background the Dashboard draws behind everything, including the nav drawer, so it can be
 * truly full-bleed (tab content is inset by the drawer's width).
 *
 * [imageUrl] is shown until [videoUrl] actually starts playing (or forever, if there's no
 * video or it fails to play).
 */
data class DashboardBackground(
    // FabricUrl (not a plain url string), so the app-wide ThumbHash placeholder factory can
    // pick up the image's hash.
    val imageUrl: FabricUrl? = null,
    val videoUrl: String? = null,
)

@Composable
private fun TabContent(
    selectedTab: Tabs,
    onBackgroundSet: (DashboardBackground?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTab != Tabs.Discover) {
        onBackgroundSet(null)
    }
    AnimatedContent(
        targetState = selectedTab,
        label = "DashboardContent",
        modifier = modifier
    ) { tab ->
        when (tab) {
            Tabs.Discover -> Discover(onBackgroundSet = {
                if (selectedTab == Tabs.Discover) {
                    // This can get called when navigating away from Discover so we need to
                    // consider the targetState
                    onBackgroundSet(it)
                }
            })

            Tabs.MyItems -> MyItems()
            Tabs.Profile -> Profile()
        }
    }
}

@Composable
private fun AnimatedBackground(background: DashboardBackground?, modifier: Modifier = Modifier) {
    val animationDuration = 300
    val videoShowing = BackgroundVideo(url = background?.videoUrl)
    // The image is drawn on top of the video: it fades out to reveal the video once it's
    // playing, and fades back in over the (still playing) outgoing video when it goes away.
    val imageAlpha by animateFloatAsState(
        targetValue = if (videoShowing) 0f else 1f,
        animationSpec = tween(durationMillis = if (videoShowing) 1000 else 500),
        label = "bgImageAlpha"
    )
    AnimatedContent(
        targetState = background?.imageUrl,
        transitionSpec = {
            // A true crossfade: both images animate together at all times. Asymmetric specs
            // (like the AnimatedContent default) dip to the blank background in between.
            fadeIn(tween(animationDuration)) togetherWith fadeOut(tween(animationDuration))
        },
        label = "bgImage",
        modifier = modifier.graphicsLayer { alpha = imageAlpha }
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

/**
 * Renders [url] as a full-bleed, muted, looping video. Returns whether the video is actually
 * attached and playing (as opposed to still loading, or failed).
 */
@Composable
private fun BackgroundVideo(url: String?): Boolean {
    // Only start video playback once the url has settled for a bit, otherwise quickly
    // browsing through Discover cards would spawn a player per property.
    var activeUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(url) {
        if (activeUrl != url) {
            activeUrl = null
            if (url != null) {
                delay(1200)
                activeUrl = url
            }
        }
    }
    // The url whose player is currently ready and attached. Can lag behind [activeUrl]: it's
    // the outgoing video's url while one is still fading out.
    var readyUrl by remember { mutableStateOf<String?>(null) }
    AnimatedContent(
        targetState = activeUrl,
        transitionSpec = {
            // No enter animation: an incoming video is revealed by the image fading out
            // above it. The exit fade keeps the outgoing video playing while the image
            // fades back in over it.
            // No sizeTransform: when the target content is empty (url == null), the default
            // one shrink-clips the exiting video down to zero instead of just fading it.
            (EnterTransition.None togetherWith fadeOut(tween(500)))
                .using(sizeTransform = null)
        },
        label = "bgVideo",
        modifier = Modifier.fillMaxSize()
    ) { videoUrl ->
        if (videoUrl != null) {
            VideoPlayer(
                url = videoUrl,
                onReadyChanged = { ready ->
                    readyUrl = when {
                        ready -> videoUrl
                        readyUrl == videoUrl -> null
                        else -> readyUrl
                    }
                }
            )
        }
    }
    return readyUrl != null && readyUrl == activeUrl
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(url: String, onReadyChanged: (Boolean) -> Unit) {
    // Only attach the PlayerView once the player is READY, so the background image stays
    // visible until the video actually has something to show (or forever, if playback fails).
    var ready by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val player = remember {
        // TODO: Fake data returns plain video urls. Once the server model is ready, hero
        //  videos will presumably be fabric links going through VideoOptionsFetcher.
        val mediaSource =
            DefaultMediaSourceFactory(context).createMediaSource(MediaItem.fromUri(url))
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaSource(mediaSource)
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                volume = 0f
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            ready = true
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("Background video error", error)
                        ready = false
                    }
                })
                prepare()
            }
    }
    LaunchedEffect(ready) { onReadyChanged(ready) }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            onReadyChanged(false)
        }
    }
    if (ready) {
        AndroidView(
            factory = {
                // Inflated from xml because surface_type can only be set through attrs.
                val playerView = LayoutInflater.from(it)
                    .inflate(R.layout.view_background_video, null) as PlayerView
                playerView.player = player
                playerView
            },
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
