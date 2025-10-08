package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.borders
import app.eluvio.wallet.theme.focusedBorder
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.util.compose.FractionBringIntoViewSpec
import app.eluvio.wallet.util.compose.RealisticDevices
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Discover(onBackgroundImageSet: (String?) -> Unit) {
    hiltViewModel<DiscoverViewModel>().subscribeToState { vm, state ->
        Discover(state, onBackgroundImageSet, vm::onPropertyClicked, vm::retry)
    }
}

@Composable
private fun Discover(
    state: DiscoverViewModel.State,
    onBackgroundImageSet: (String?) -> Unit,
    onPropertyClicked: (MediaPropertyEntity) -> Unit,
    onRetryClicked: () -> Unit,
) {
    BoxWithConstraints(
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
            DiscoverGrid(
                state,
                onPropertyFocused = { property ->
                    onBackgroundImageSet(property.bgImageWithFallback?.url)
                },
                onPropertyClicked = onPropertyClicked,
                onRetryClicked = onRetryClicked
            )
        }
    }
}

@Composable
fun SinglePropertyPage(
    state: DiscoverViewModel.State,
    onBackgroundImageSet: (String?) -> Unit,
    onPropertyClicked: (MediaPropertyEntity) -> Unit,
    onRetryClicked: () -> Unit
) {
    val property = state.properties.firstOrNull()
    LaunchedEffect(property?.startScreenBackground) {
        onBackgroundImageSet(property?.startScreenBackground?.url)
    }
    if (state.loading) {
        EluvioLoadingSpinner(Modifier.padding(top = 100.dp))
    } else if (property != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            AsyncImage(
                property.startScreenLogo,
                contentDescription = "${property.name} Logo",
                modifier = Modifier.height(160.dp)
            )
            TvButton(
                if (state.isLoggedIn) "Welcome Back" else "Sign In",
                onClick = { onPropertyClicked(property) },
                Modifier.requestInitialFocus()
            )
        }
    } else if (state.showRetryButton) {
        RetryButton(onRetryClicked, Modifier.padding(top = 100.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxWithConstraintsScope.DiscoverGrid(
    state: DiscoverViewModel.State,
    onPropertyFocused: (MediaPropertyEntity) -> Unit,
    onPropertyClicked: (MediaPropertyEntity) -> Unit,
    onRetryClicked: () -> Unit
) {
    val width by rememberUpdatedState(maxWidth)
    val horizontalPadding = 50.dp
    val cardSpacing = 15.dp
    val desiredCardWidth = 170.dp
    val columnCount = remember(width, horizontalPadding, desiredCardWidth, cardSpacing) {
        val availableWidth = width - horizontalPadding
        val cardWidth = desiredCardWidth + cardSpacing
        (availableWidth / cardWidth).roundToInt()
    }
    val scrollState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // The focus problems here were hard to solve, I got some hints from:
    // https://stackoverflow.com/questions/76281554/android-jetpack-compose-tv-focus-restoring
    // But ultimately I had to make some adjustments because we also wanted to restore focus when
    // moving to the nav drawer and coming back, as well as focusing the first item on launch.

    /**
     * Always keep track of the last focused property, but there's more logic involved in actually
     * restoring focus to it.
     */
    val currentFocusedProperty = rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * We save a clicked property, so we can restore focus to it when navigating back to this
     * screen. This is different than restoring focus when navigating between other elements on screen.
     */
    val lastClickedProperty = rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * This is a trigger to let the corresponding Property item know that it should request focus
     * right now.
     */
    val onDemandFocusRestore = rememberSaveable { mutableStateOf<String?>(null) }

    val bivs = remember { FractionBringIntoViewSpec(parentFraction = 0.45f) }
    val properties = state.properties
    CompositionLocalProvider(LocalBringIntoViewSpec provides bivs) {
        LazyVerticalGrid(
            state = scrollState,
            columns = GridCells.Fixed(columnCount),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            verticalArrangement = Arrangement.spacedBy(cardSpacing),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .fillMaxHeight()
                .onFocusChanged {
                    if (it.hasFocus && lastClickedProperty.value == null) {
                        // We're gaining focus, but don't have a last clicked property: this means we
                        // are gaining focus back from an element on screen, rather than coming back
                        // from a different screen.
                        onDemandFocusRestore.value = currentFocusedProperty.value
                    }
                }
                .onPreviewKeyEvent {
                    val firstPropertyId = properties.firstOrNull()?.id
                    if (firstPropertyId != null && it.isKeyUpOf(Key.Back) && currentFocusedProperty.value != firstPropertyId) {
                        // User clicked back while not focused on first item. Scroll to top and
                        // trigger focus request.
                        scope.launch {
                            scrollState.animateScrollToItem(0)
                        }
                        onDemandFocusRestore.value = firstPropertyId
                        return@onPreviewKeyEvent true
                    }
                    false
                }
        ) {
            item(contentType = { "header" }, span = { GridItemSpan(maxLineSpan) }) {
                Image(
                    painter = painterResource(id = R.drawable.discover_logo),
                    contentDescription = "Eluvio Logo",
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .padding(top = Overscan.verticalPadding)
                        .height(105.dp)
                )
            }

            if (state.loading) {
                item(contentType = { "spinner" }, span = { GridItemSpan(maxLineSpan) }) {
                    EluvioLoadingSpinner(Modifier.padding(top = 100.dp))
                }
            } else if (state.properties.isNotEmpty()) {
                itemsIndexed(
                    properties,
                    contentType = { _, _ -> "property_card" },
                    key = { _, property -> property.id }
                ) { index, property ->
                    PropertyCard(
                        index = index,
                        property = property,
                        scrollState = scrollState,
                        lastClickedProperty = lastClickedProperty,
                        currentFocusedProperty = currentFocusedProperty,
                        onDemandFocusRestore = onDemandFocusRestore,
                        onPropertyClicked = onPropertyClicked,
                        onPropertyFocused = onPropertyFocused
                    )
                }
            } else if (state.showRetryButton) {
                item(contentType = "button", span = { GridItemSpan(maxLineSpan) }) {
                    RetryButton(onRetryClicked, Modifier.padding(top = 100.dp))
                }
            } else {
                // Technically unreachable code, because [loading = (properties.isEmpty())]
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "label") {
                    Text(stringResource(R.string.no_content_warning))
                }
            }
            item(contentType = { "footer" }, span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PropertyCard(
    index: Int,
    property: MediaPropertyEntity,
    scrollState: LazyGridState,
    lastClickedProperty: MutableState<String?>,
    currentFocusedProperty: MutableState<String?>,
    onDemandFocusRestore: MutableState<String?>,
    onPropertyClicked: (MediaPropertyEntity) -> Unit,
    onPropertyFocused: (MediaPropertyEntity) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = {
            lastClickedProperty.value = property.id
            onPropertyClicked(property)
        },
        border = MaterialTheme.borders.focusedBorder,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(2.dp)),
        modifier = Modifier
            // Assume aspect ratio,
            // this will avoid un-selectable cards
            .aspectRatio(AspectRatio.POSTER)
            .focusRequester(focusRequester)
            .thenIf(property.id == lastClickedProperty.value) {
                onGloballyPositioned {
                    Log.e("Restoring focus after navigation to ${property.id} ")
                    focusRequester.requestFocus()
                }
            }
            .onFocusChanged {
                if (it.hasFocus) {
                    currentFocusedProperty.value = property.id
                    // When any items gains focus, clear lastClickedProperty. It either
                    // doesn't need handling, or has already been handled.
                    lastClickedProperty.value = null
                    onPropertyFocused(property)
                }
            }
    ) {
        var showImage by remember(property.image) { mutableStateOf(true) }
        if (showImage) {
            ShimmerImage(
                model = property.image,
                contentDescription = property.name,
                onError = { showImage = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = property.name,
                style = MaterialTheme.typography.label_40.copy(
                    fontSize = 22.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(10.dp)
            )
        }
    }
    LaunchedEffect(property.id == onDemandFocusRestore.value) {
        if (property.id == onDemandFocusRestore.value) {
            Log.e("On-demand focus restore for: ${property.id}")
            onDemandFocusRestore.value = null
            focusRequester.requestFocus()
            // +1 because header is at index 0
            scrollState.animateScrollToItem(index + 1)
        }
    }
}

@Composable
private fun RetryButton(onRetryClicked: () -> Unit, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center) {
        TvButton(
            onClick = onRetryClicked,
            modifier = modifier
        ) {
            Row(
                Modifier.padding(
                    top = 5.dp,
                    bottom = 5.dp,
                    start = 20.dp,
                    end = 14.dp
                )
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.label_40,
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear",
                    Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverPreview() = EluvioThemePreview {
    Discover(
        DiscoverViewModel.State(
            loading = false,
            isLoggedIn = false,
            properties = (1..50).map {
                MediaPropertyEntity().apply {
                    id = "$it"
                    name = "Property #$it"
                }
            }
        ),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverLoadingPreview() = EluvioThemePreview {
    Discover(
        DiscoverViewModel.State(loading = true, isLoggedIn = false),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverEmptyPreview() = EluvioThemePreview {
    Discover(
        DiscoverViewModel.State(loading = false, isLoggedIn = false),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun DiscoverRetryPreview() = EluvioThemePreview {
    Discover(
        DiscoverViewModel.State(loading = false, isLoggedIn = false, showRetryButton = true),
        onBackgroundImageSet = {},
        onPropertyClicked = {},
        onRetryClicked = {},
    )
}
