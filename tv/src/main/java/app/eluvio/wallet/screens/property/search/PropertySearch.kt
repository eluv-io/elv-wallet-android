package app.eluvio.wallet.screens.property.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.search.SearchFilter
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.SearchBox
import app.eluvio.wallet.screens.common.SearchFilterChip
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.sections
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.util.compose.LocalCardTheme
import app.eluvio.wallet.util.subscribeToState
import kotlinx.collections.immutable.persistentListOf

@Composable
fun PropertySearch(vm: PropertySearchViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    vm.subscribeToState(
        onState = { vm, state ->
            BackHandler(enabled = state.handleBackPress) { vm.onBackPressed() }
            PropertySearch(
                state,
                query,
                onQueryChanged = {
                    query = it
                    vm.onQueryChanged(it)
                },
                onPrimaryFilterClick = vm::onPrimaryFilterClick,
                onSecondaryFilterClick = vm::onSecondaryFilterClick,
                onSearchClicked = vm::onSearchClicked,
            )
        },
        onEvent = {
            if (it is ResetQueryEvent) {
                query = ""
                true
            } else {
                false
            }
        }
    )
}

@Composable
private fun PropertySearch(
    state: PropertySearchViewModel.State,
    query: String,
    onQueryChanged: (String) -> Unit,
    onPrimaryFilterClick: (SearchFilter.Value) -> Unit,
    onSecondaryFilterClick: (SearchFilter.Value) -> Unit,
    onSearchClicked: () -> Unit,
) {
    val bgModifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(listOf(Color(0xFF16151F), Color(0xFF0C0C10))))
    if (state.loading) {
        LoadingSpinner(modifier = bgModifier)
        return
    }
    LazyColumn(modifier = bgModifier) {
        item(contentType = "header") { Header(state, query, onQueryChanged, onSearchClicked) }
        spacer(8.dp)
        item(contentType = "filter_selector") {
            FilterSelector(state, onPrimaryFilterClick, onSecondaryFilterClick)
        }
        if (state.loadingResults) {
            item(contentType = "loading_spinner") {
                LoadingSpinner(
                    Modifier
                        .fillParentMaxWidth()
                        .padding(150.dp)
                )
            }
        } else {
            sections(state.searchResults)
        }
    }
}

@Composable
private fun LoadingSpinner(modifier: Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        EluvioLoadingSpinner()
    }
}

@Composable
fun FilterSelector(
    state: PropertySearchViewModel.State,
    onPrimaryFilterClick: (SearchFilter.Value) -> Unit,
    onSecondaryFilterClick: (SearchFilter.Value) -> Unit
) {
    val primaryFilter = state.primaryFilter
    val selectedFilters = state.selectedFilters
    if (primaryFilter == null || primaryFilter.values.isEmpty()) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
    ) {
        FiltersRow(
            filter = primaryFilter,
            selectedFilterValue = selectedFilters?.primaryFilterValue,
            onClick = onPrimaryFilterClick,
            label = "Filters"
        )

        val secondaryFilters = selectedFilters?.secondaryFilterAttribute
        if (secondaryFilters != null && secondaryFilters.values.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FiltersRow(
                filter = secondaryFilters,
                selectedFilterValue = selectedFilters.secondaryFilterValue,
                onClick = onSecondaryFilterClick,
            )
        }
    }
}

@Composable
private fun FiltersRow(
    filter: SearchFilter,
    selectedFilterValue: String?,
    onClick: (SearchFilter.Value) -> Unit,
    label: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.width(Overscan.horizontalPadding))
        if (label != null) {
            Text(label, style = MaterialTheme.typography.carousel_36)
            Spacer(Modifier.width(16.dp))
        }
        val filterPadding = 16.dp
        filter.values.forEach { filterValue ->
            val selected = selectedFilterValue == filterValue.value
            if (filter.style == SearchFilter.Style.IMAGE && filterValue.imageUrl != null) {
                FilterImageCard(
                    filterValue = filterValue,
                    selected = selected,
                    cardTheme = filter.cardTheme,
                    onClick = onClick
                )
            } else {
                SearchFilterChip(
                    title = filterValue.value,
                    value = filterValue,
                    selected = selected,
                    onClick = onClick,
                    onFocus = {})
            }
            Spacer(Modifier.width(16.dp))
        }
        Spacer(Modifier.width(Overscan.horizontalPadding - filterPadding))
    }
}

/** The height every filter image card is laid out at. Width follows the image's own shape. */
private val FilterImageHeight = 56.dp

/** How far an unselected, unfocused filter is dimmed. */
private const val UnselectedFilterAlpha = 0.3f

/**
 * A filter value rendered as an image card, themed by the Property's card theme like any section
 * item. Unselected cards sit dimmed until focused, matching the web's inactive filters.
 */
@Composable
private fun FilterImageCard(
    filterValue: SearchFilter.Value,
    selected: Boolean,
    cardTheme: CardThemeEntity?,
    onClick: (SearchFilter.Value) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    // Filters don't declare an aspect ratio, so each card takes the shape of its own image.
    // Until it loads there's nothing to measure, and the card has to start at the same ratio its
    // box falls back to: a null ratio would lay the card out square but stop the theme
    // circularizing it, so every card would start as a rounded rect and pop into a circle.
    var aspectRatio by remember(filterValue.imageUrl) { mutableStateOf(AspectRatio.SQUARE) }
    Box(
        modifier = Modifier
            .onFocusChanged { focused = it.hasFocus }
            // Dimming introduces a graphics layer, which clips drawing to its bounds. A theme
            // draws its border straddling the card's bounds, so half the stroke lands outside
            // them and would be sliced off wherever the shape meets its box - the top and bottom
            // of a circle, say. Reserving half the stroke around the card keeps it whole.
            .alpha(if (selected || focused) 1f else UnselectedFilterAlpha)
            .padding(((cardTheme?.borderWidth ?: 0) / 2f).dp)
    ) {
        CompositionLocalProvider(LocalCardTheme provides cardTheme) {
            ImageCard(
                imageUrl = filterValue.imageUrl,
                contentDescription = filterValue.value,
                aspectRatio = aspectRatio,
                // Like tvOS, a focused filter is just lit up and scaled - none of the card focus
                // treatment (sheen, scrim, sweeping ring) applies to it.
                respondToFocus = false,
                showFocusRing = false,
                onImageSuccess = {
                    it.painter.intrinsicSize.toAspectRatio()?.let { ratio -> aspectRatio = ratio }
                },
                onClick = { onClick(filterValue) },
                modifier = Modifier
                    .height(FilterImageHeight)
                    .aspectRatio(aspectRatio)
            )
        }
    }
}

/** The ratio of a loaded image, or null when it doesn't report a usable size. */
private fun Size.toAspectRatio(): Float? =
    takeIf { it.isSpecified && it.height > 0f }?.let { it.width / it.height }

@Composable
private fun Header(
    state: PropertySearchViewModel.State,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    Column(Modifier.padding(Overscan.defaultPadding(excludeBottom = true))) {
        SearchBox(
            query,
            hint = "Search ${state.propertyName}",
            onQueryChanged,
            onSearchClicked,
        )
        Spacer(Modifier.height(2.dp))
        HorizontalDivider()
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun PropertySearchPreview() = EluvioThemePreview {
    val primaryFilter = SearchFilter(
        id = "primary",
        title = "Primary Filter",
        values = List(4) {
            SearchFilter.Value("Primary Filter Value $it")
        },
        style = SearchFilter.Style.TEXT
    )
    val secondaryFilter = SearchFilter(
        id = "secondary",
        title = "Secondary Filter",
        values = List(4) {
            SearchFilter.Value("Secondary Filter $it", imageUrl = object : FabricUrl {
                override val url: String get() = "dummy"
                override val imageHash: String? = null
            })
        },
        style = SearchFilter.Style.IMAGE
    )
    PropertySearch(
        PropertySearchViewModel.State(
            loading = false,
            propertyName = "FlixVerse",
            primaryFilter = primaryFilter,
            selectedFilters = PropertySearchViewModel.State.SelectedFilters(
                primaryFilter,
                primaryFilter.values.first().value,
                secondaryFilter,
                secondaryFilter.values[1].value
            ),
            searchResults = listOf(
                DynamicPageLayoutState.Section.Carousel(
                    permissionContext = PermissionContext(propertyId = "p", sectionId = "4"),
                    displaySettings = SimpleDisplaySettings(
                        title = "Carousel",
                        subtitle = "Subtitle",
                        displayFormat = DisplayFormat.CAROUSEL,
                    ),
                    items = persistentListOf(
                        DynamicPageLayoutState.CarouselItem.Media(
                            permissionContext = PermissionContext(propertyId = "property1"),
                            forceDisabled = false,
                            entity = MediaEntity().apply {
                                id = "1"
                                name = "Media 1"
                                mediaType = "image"
                            },
                            playbackProgress = null,
                        ),
                        DynamicPageLayoutState.CarouselItem.RedeemableOffer(
                            permissionContext = PermissionContext(propertyId = "property1"),
                            forceDisabled = false,
                            offerId = "1",
                            name = "Offer 1",
                            fulfillmentState = RedeemableOfferEntity.FulfillmentState.AVAILABLE,
                            contractAddress = "0x123",
                            tokenId = "1",
                            imageUrl = null,
                            animation = null
                        )
                    )
                )
            )
        ),
        query = "",
        onPrimaryFilterClick = {},
        onSecondaryFilterClick = {},
        onQueryChanged = {},
        onSearchClicked = {},
    )
}
