package app.eluvio.wallet.screens.dashboard.discover

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.dashboard.discover.DiscoverViewModel.State
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.util.compose.FractionBringIntoViewSpec
import app.eluvio.wallet.util.compose.RealisticDevices
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.subscribeToState
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun Discover(onBackgroundImageSet: (FabricUrl?) -> Unit) {
    hiltViewModel<DiscoverViewModel>().subscribeToState { vm, state ->
        Discover(state, onBackgroundImageSet, vm::onPropertyClicked, vm::retry)
    }
}

@Composable
private fun Discover(
    state: State,
    onBackgroundImageSet: (FabricUrl?) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
    onRetryClicked: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.singlePropertyMode) {
            SinglePropertyPage(
                state,
                onBackgroundImageSet = onBackgroundImageSet,
                onPropertyClicked = onPropertyClicked,
                onRetryClicked = onRetryClicked
            )
        } else {
            // The redesigned Discover page draws its own hero background.
            LaunchedEffect(Unit) { onBackgroundImageSet(null) }
            DiscoverPage(
                state,
                onPropertyClicked = onPropertyClicked,
                onRetryClicked = onRetryClicked
            )
        }
    }
}

/**
 * The redesigned Discover page: full-bleed hero (video/image) driven by the focused property,
 * property logo + action buttons, and categorized rows of property cards.
 */
@Composable
private fun DiscoverPage(
    state: State,
    onPropertyClicked: (State.Property) -> Unit,
    onRetryClicked: () -> Unit,
) {
    if (state.loading) {
        EluvioLoadingSpinner()
        return
    }
    if (state.showRetryButton) {
        RetryButton(onRetryClicked)
        return
    }
    if (state.rows.isEmpty()) {
        Text(stringResource(R.string.no_content_warning))
        return
    }

    var focusedProperty by remember { mutableStateOf<State.Property?>(null) }
    var browsingRows by remember { mutableStateOf(false) }
    val displayedProperty = focusedProperty
        ?: state.rows.firstOrNull()?.properties?.firstOrNull()

    Box(Modifier.fillMaxSize()) {
        DiscoverHero(displayedProperty)
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 70.dp)
        ) {
            PropertyLogo(displayedProperty, Modifier.padding(start = 5.dp))
            HeroButtons(
                property = displayedProperty,
                // While browsing rows, the primary action is shown pre-selected, hinting at
                // what pressing "up" will focus.
                highlightPrimary = browsingRows,
                onPropertyClicked = onPropertyClicked,
                modifier = Modifier.padding(start = 5.dp, top = 22.dp, bottom = 10.dp)
            )
            DiscoverRows(
                rows = state.rows,
                onPropertyFocused = { focusedProperty = it },
                onPropertyClicked = onPropertyClicked,
                modifier = Modifier.onFocusChanged { browsingRows = it.hasFocus }
            )
        }
    }
}

@Composable
private fun DiscoverHero(property: State.Property?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Crossfade(
            targetState = property?.focusBackgroundUrl,
            label = "Hero background"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        HeroVideo(property?.heroVideoUrl)
        // Left scrim, so logo/buttons stay readable over the hero.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to HeroBaseColor.copy(alpha = 0.96f),
                        0.26f to HeroBaseColor.copy(alpha = 0.72f),
                        0.52f to HeroBaseColor.copy(alpha = 0.15f),
                        0.72f to Color.Transparent,
                    )
                )
        )
        // Bottom scrim, so the rows stay readable over the hero.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        0.74f to HeroBaseColor.copy(alpha = 0.55f),
                        0.98f to HeroBaseColor.copy(alpha = 0.98f),
                    )
                )
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun HeroVideo(videoUrl: String?) {
    // Only start video playback once focus has settled on a property for a bit,
    // otherwise quickly browsing through cards would spawn a player per property.
    var activeUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(videoUrl) {
        if (activeUrl != videoUrl) {
            activeUrl = null
            if (videoUrl != null) {
                delay(1200)
                activeUrl = videoUrl
            }
        }
    }
    val url = activeUrl ?: return
    val context = LocalContext.current
    key(url) {
        // PlayerView's SurfaceView "punches a hole" through the window, hiding the hero image
        // even before the video has anything to show. Only attach the PlayerView once the
        // player is READY, so the hero image stays visible until then (or forever, if
        // playback fails).
        var ready by remember { mutableStateOf(false) }
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
                            Log.e("Hero video error", error)
                            ready = false
                        }
                    })
                    prepare()
                }
        }
        DisposableEffect(Unit) {
            onDispose { player.release() }
        }
        if (ready) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PropertyLogo(property: State.Property?, modifier: Modifier = Modifier) {
    Box(modifier.height(86.dp), contentAlignment = Alignment.BottomStart) {
        Crossfade(targetState = property, label = "Property logo") { prop ->
            val logoUrl = prop?.logo?.url
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = prop.name,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.4f)
                    )
                } else if (prop != null) {
                    Text(
                        text = prop.name,
                        style = MaterialTheme.typography.label_40.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F4F5),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroButtons(
    property: State.Property?,
    highlightPrimary: Boolean,
    onPropertyClicked: (State.Property) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasProgress = property?.hasWatchProgress == true
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        HeroButton(
            text = if (hasProgress) "Resume" else "Explore",
            icon = if (hasProgress) Icons.Default.PlayArrow else Icons.Default.Search,
            highlight = highlightPrimary,
            onClick = { property?.let(onPropertyClicked) },
            modifier = Modifier.requestInitialFocus()
        )
        if (hasProgress) {
            HeroButton(
                text = "More Info",
                icon = Icons.Outlined.Info,
                highlight = false,
                onClick = { property?.let(onPropertyClicked) },
            )
        }
    }
}

@Composable
private fun HeroButton(
    text: String,
    icon: ImageVector,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (highlight) ButtonFocusedContainer else ButtonContainer
    val content = if (highlight) ButtonFocusedContent else ButtonContent
    TvButton(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = ButtonFocusedContainer,
            focusedContentColor = ButtonFocusedContent,
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.label_40.copy(fontSize = 11.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverRows(
    rows: List<State.Row>,
    onPropertyFocused: (State.Property) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pin the focused row near the top of the viewport (rows "rise" as you move down).
    // The fraction leaves enough room above the focused card for the row title
    // (~18dp text + 4dp margin + 12dp of LazyRow padding) plus the top fading edge.
    val verticalSpec = remember { FractionBringIntoViewSpec(parentFraction = 0.13f) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides verticalSpec) {
        LazyColumn(
            // Top padding keeps the first row's title clear of the top fading edge, and lines
            // it up with where BringIntoView pins the other rows' titles.
            contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp),
            modifier = modifier
                .height(342.dp)
                .verticalFadingEdges()
        ) {
            itemsIndexed(
                rows,
                contentType = { _, _ -> "discover_row" },
                key = { index, row -> "$index:${row.title}" }
            ) { _, row ->
                DiscoverRow(row, onPropertyFocused, onPropertyClicked)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun DiscoverRow(
    row: State.Row,
    onPropertyFocused: (State.Property) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
) {
    Column {
        Text(
            text = row.title,
            style = MaterialTheme.typography.label_40.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Normal,
            color = Color(0xFFF4F4F5),
            modifier = Modifier.padding(start = 5.dp, bottom = 4.dp)
        )
        val horizontalSpec = remember { FractionBringIntoViewSpec(parentFraction = 0.02f) }
        val firstItemFocusRequester = remember { FocusRequester() }
        CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalSpec) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                // Vertical padding leaves room for the focused-card scale to draw without
                // clipping, horizontal padding does the same for the first/last cards.
                contentPadding = PaddingValues(start = 5.dp, end = 75.dp, top = 12.dp, bottom = 12.dp),
                // When the row (re)gains focus, land on its last-focused card instead of
                // whatever card happens to sit under the previous row's focus position.
                // Rows that never held focus start at their first card.
                modifier = Modifier.focusRestorer { firstItemFocusRequester }
            ) {
                itemsIndexed(
                    row.properties,
                    contentType = { _, _ -> "property_card" },
                    key = { _, property -> property.id }
                ) { index, property ->
                    PropertyCard(
                        property = property,
                        onPropertyFocused = onPropertyFocused,
                        onPropertyClicked = onPropertyClicked,
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyCard(
    property: State.Property,
    onPropertyFocused: (State.Property) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = { onPropertyClicked(property) },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(CardCornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = CardBackground,
            focusedContainerColor = CardBackground,
        ),
        modifier = modifier
            .size(width = 116.dp, height = 174.dp)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    onPropertyFocused(property)
                }
            }
    ) {
        var showImage by remember(property.cardImage) { mutableStateOf(true) }
        if (showImage) {
            ShimmerImage(
                model = property.cardImage,
                contentDescription = property.name,
                contentScale = ContentScale.Crop,
                onError = { showImage = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = property.name,
                style = MaterialTheme.typography.label_40.copy(
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(10.dp)
            )
        }
        if (focused) {
            // Top "sheen" highlight on the focused card.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.45f),
                            0.16f to Color.White.copy(alpha = 0.16f),
                            0.42f to Color.Transparent,
                        )
                    )
            )
            AnimatedFocusRing(cornerRadius = CardCornerRadius)
        }
    }
}

/**
 * The focused-card ring from the design: a thin white stroke whose bright segment sweeps
 * around the card, one revolution per 3.6s.
 *
 * Compose's [Brush.sweepGradient] can't rotate its start angle, so the ring is drawn with a
 * framework [SweepGradient] whose local matrix is rotated each frame. The angle is only read
 * at draw time, so the animation invalidates the draw phase without recomposing.
 */
@Composable
private fun AnimatedFocusRing(cornerRadius: Dp, modifier: Modifier = Modifier) {
    val angle by rememberInfiniteTransition(label = "focusRing")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
            label = "ringAngle"
        )
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val strokeWidth = 1.dp.toPx()
                val inset = strokeWidth / 2
                val rect = RectF(inset, inset, size.width - inset, size.height - inset)
                val radius = cornerRadius.toPx() - inset
                val matrix = Matrix()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    shader = SweepGradient(
                        size.width / 2,
                        size.height / 2,
                        intArrayOf(RingBaseColor, RingPeakColor, RingBaseColor, RingBaseColor),
                        // Bright segment peaks at 90° and fades back out by 200°.
                        floatArrayOf(0f, 90 / 360f, 200 / 360f, 1f)
                    )
                }
                onDrawBehind {
                    // The design's conic gradient starts at 12 o'clock; SweepGradient starts
                    // at 3 o'clock, so shift by -90°.
                    matrix.setRotate(angle - 90f, size.width / 2, size.height / 2)
                    paint.shader.setLocalMatrix(matrix)
                    drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, paint) }
                }
            }
    )
}

/**
 * Fades out content near the top and bottom edges of the rows viewport.
 */
private fun Modifier.verticalFadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.03f to Color.Black,
                0.55f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn
        )
    }

private val HeroBaseColor = Color(0xFF08090C)
private val CardBackground = Color(0xFF15161A)
private val CardCornerRadius = 6.dp

// Framework colors for the focus ring's SweepGradient shader.
private val RingBaseColor = android.graphics.Color.argb(41, 255, 255, 255) // white @ 16%
private val RingPeakColor = android.graphics.Color.WHITE
private val ButtonContainer = Color(0x6B787882)
private val ButtonContent = Color(0xFFF4F4F5)
private val ButtonFocusedContainer = Color(0xFFF4F4F5)
private val ButtonFocusedContent = Color(0xFF0A0A0B)

private fun previewState() = State(
    loading = false,
    isLoggedIn = false,
    rows = (1..4).map { rowIndex ->
        State.Row(
            title = "Row $rowIndex",
            properties = (1..15).map {
                State.Property(
                    id = "$rowIndex-$it",
                    name = "Property $it",
                    loginProvider = "ory",
                    skipLogin = false,
                    cardImage = null,
                    focusBackgroundUrl = null,
                    logo = null,
                    heroVideoUrl = null,
                    hasWatchProgress = it % 2 == 0,
                    startScreenLogo = null,
                    startScreenBackground = null
                )
            }
        )
    }
)

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverPreview() = EluvioThemePreview {
    Discover(
        previewState(),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverLoadingPreview() = EluvioThemePreview {
    Discover(
        State(loading = true, isLoggedIn = false),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverEmptyPreview() = EluvioThemePreview {
    Discover(
        State(loading = false, isLoggedIn = false),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverRetryPreview() = EluvioThemePreview {
    Discover(
        State(loading = false, isLoggedIn = false, showRetryButton = true),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}
