package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.screens.common.AnimatedFocusRing
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.dashboard.DashboardBackground
import app.eluvio.wallet.screens.dashboard.discover.DiscoverViewModel.State
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.util.compose.FractionBringIntoViewSpec
import app.eluvio.wallet.util.compose.RealisticDevices
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.subscribeToState
import coil3.compose.AsyncImage

@Composable
fun Discover(onBackgroundSet: (DashboardBackground?) -> Unit) {
    hiltViewModel<DiscoverViewModel>().subscribeToState { vm, state ->
        Discover(
            state,
            onBackgroundSet,
            vm::onPropertyFocused,
            vm::onPropertyClicked,
            vm::retry
        )
    }
}

@Composable
private fun Discover(
    state: State,
    onBackgroundSet: (DashboardBackground?) -> Unit,
    onPropertyFocused: (State.Property) -> Unit,
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
                onBackgroundSet = onBackgroundSet,
                onPropertyClicked = onPropertyClicked,
                onRetryClicked = onRetryClicked
            )
        } else {
            DiscoverPage(
                state,
                onBackgroundSet = onBackgroundSet,
                onPropertyFocused = onPropertyFocused,
                onPropertyClicked = onPropertyClicked,
                onRetryClicked = onRetryClicked
            )
        }
    }
}

/**
 * The redesigned Discover page: full-bleed hero (video/image) driven by the focused property,
 * property logo, and categorized rows of property cards.
 */
@Composable
private fun DiscoverPage(
    state: State,
    onBackgroundSet: (DashboardBackground?) -> Unit,
    onPropertyFocused: (State.Property) -> Unit,
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
    val displayedProperty = focusedProperty
        ?: state.rows.firstOrNull()?.properties?.firstOrNull()

    // The hero itself is drawn by the Dashboard, where it can be truly full-bleed
    // (extend under the nav drawer).
    LaunchedEffect(displayedProperty, state.heroVideo) {
        onBackgroundSet(displayedProperty?.let {
            DashboardBackground(imageUrl = it.focusBackgroundUrl, video = state.heroVideo)
        })
    }

    /**
     * The card ("rowIndex:propertyId") that was clicked to navigate away. When coming back to
     * this screen, that card grabs focus again — otherwise nothing here is focused and the nav
     * drawer steals focus (and opens). Cleared as soon as any card gains focus.
     */
    val lastClickedCard = rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        HeroScrims()
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 70.dp)
        ) {
            PropertyLogo(displayedProperty, Modifier.padding(start = 5.dp, bottom = 22.dp))
            PropertyText(displayedProperty, Modifier.padding(start = 5.dp))
            DiscoverRows(
                rows = state.rows,
                lastClickedCard = lastClickedCard,
                onPropertyFocused = {
                    focusedProperty = it
                    onPropertyFocused(it)
                },
                onPropertyClicked = onPropertyClicked,
            )
        }
    }
}

/**
 * Scrims over the hero background, so the logo/buttons/rows stay readable.
 *
 * The hero is drawn full-bleed by the Dashboard, but this screen is inset by the nav drawer's
 * width — so the scrims draw past their left edge to cover the full screen, otherwise a bright
 * unscrimmed strip of hero would show at the drawer's edge.
 */
@Composable
private fun HeroScrims(modifier: Modifier = Modifier) {
    val drawerWidth = NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
    Spacer(
        modifier
            .fillMaxSize()
            .drawBehind {
                val left = -drawerWidth.toPx()
                val topLeft = Offset(left, 0f)
                val fullSize = Size(size.width - left, size.height)
                // Left scrim, so logo/buttons stay readable over the hero.
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to HeroBaseColor.copy(alpha = 0.96f),
                        0.26f to HeroBaseColor.copy(alpha = 0.72f),
                        0.52f to HeroBaseColor.copy(alpha = 0.15f),
                        0.72f to Color.Transparent,
                        startX = left,
                        endX = left + fullSize.width,
                    ),
                    topLeft = topLeft,
                    size = fullSize,
                )
                // Bottom scrim, so the rows stay readable over the hero.
                drawRect(
                    brush = Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        0.74f to HeroBaseColor.copy(alpha = 0.55f),
                        0.98f to HeroBaseColor.copy(alpha = 0.98f),
                    ),
                    topLeft = topLeft,
                    size = fullSize,
                )
            }
    )
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

/**
 * The property's Discover-page title and description, under its logo.
 * Most properties don't define either, in which case this takes up no space at all.
 */
@Composable
private fun PropertyText(property: State.Property?, modifier: Modifier = Modifier) {
    Crossfade(targetState = property, modifier = modifier, label = "Property text") { prop ->
        val title = prop?.mainPageTitle
        val description = prop?.mainPageDescription
        if (title == null && description == null) return@Crossfade
        Column(
            Modifier
                .fillMaxWidth(0.42f)
                .padding(bottom = 22.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.label_40.copy(fontSize = 16.sp),
                    color = Color(0xFFF4F4F5),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.body_32.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFFB4B6BD),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverRows(
    rows: List<State.Row>,
    lastClickedCard: MutableState<String?>,
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
            ) { rowIndex, row ->
                DiscoverRow(rowIndex, row, lastClickedCard, onPropertyFocused, onPropertyClicked)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun DiscoverRow(
    rowIndex: Int,
    row: State.Row,
    lastClickedCard: MutableState<String?>,
    onPropertyFocused: (State.Property) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
) {
    Column {
        // Rows aren't required to have a title.
        if (row.title.isNotEmpty()) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.label_40.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Normal,
                color = Color(0xFFF4F4F5),
                modifier = Modifier.padding(start = 5.dp, bottom = 4.dp)
            )
        }
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
                        // The same property can appear in multiple rows, so scope the key per row.
                        focusKey = "$rowIndex:${property.id}",
                        lastClickedCard = lastClickedCard,
                        onPropertyFocused = onPropertyFocused,
                        onPropertyClicked = onPropertyClicked,
                        modifier = when {
                            // The very first card takes initial focus, so the nav drawer doesn't.
                            rowIndex == 0 && index == 0 ->
                                Modifier.requestInitialFocus(firstItemFocusRequester)

                            index == 0 -> Modifier.focusRequester(firstItemFocusRequester)
                            else -> Modifier
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
    focusKey: String,
    lastClickedCard: MutableState<String?>,
    onPropertyFocused: (State.Property) -> Unit,
    onPropertyClicked: (State.Property) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = {
            lastClickedCard.value = focusKey
            onPropertyClicked(property)
        },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(CardCornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = CardBackground,
            focusedContainerColor = CardBackground,
        ),
        modifier = modifier
            .size(width = 116.dp, height = 174.dp)
            .focusRequester(focusRequester)
            .thenIf(focusKey == lastClickedCard.value) {
                // Restore focus when coming back from a screen this card navigated to.
                // Requesting focus on the already-focused card right after the click is a
                // no-op, so [lastClickedCard] survives until we actually leave and return.
                onGloballyPositioned { focusRequester.requestFocus() }
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    // Focus moved somewhere on this screen: restoration is either done or
                    // no longer relevant.
                    lastClickedCard.value = null
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
            AnimatedFocusRing(RoundedCornerShape(CardCornerRadius))
        }
    }
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
                    mainPageTitle = "Property $it Title",
                    mainPageDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing " +
                            "elit. Sed do eiusmod tempor incididunt ut labore.",
                    heroVideoHash = null,
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
        onBackgroundSet = {},
        onPropertyFocused = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverLoadingPreview() = EluvioThemePreview {
    Discover(
        State(loading = true, isLoggedIn = false),
        onBackgroundSet = {},
        onPropertyFocused = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverEmptyPreview() = EluvioThemePreview {
    Discover(
        State(loading = false, isLoggedIn = false),
        onBackgroundSet = {},
        onPropertyFocused = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverRetryPreview() = EluvioThemePreview {
    Discover(
        State(loading = false, isLoggedIn = false, showRetryButton = true),
        onBackgroundSet = {},
        onPropertyFocused = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}
