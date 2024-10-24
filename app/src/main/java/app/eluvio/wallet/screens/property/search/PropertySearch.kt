package app.eluvio.wallet.screens.property.search

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.search.SearchFilter
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.SearchBox
import app.eluvio.wallet.screens.common.SearchFilterChip
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.property.sections
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination

@MainGraph
@Destination(navArgsDelegate = PropertySearchNavArgs::class)
@Composable
fun PropertySearch() {
    var query by rememberSaveable { mutableStateOf("") }
    hiltViewModel<PropertySearchViewModel>().subscribeToState(
        onState = { vm, state ->
            BackHandler { vm.onBackPressed() }
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
    if (state.primaryFilter == null || state.primaryFilter.values.isEmpty()) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        FiltersRow(
            filter = state.primaryFilter,
            selectedFilterValue = state.selectedFilters?.primaryFilterValue,
            onClick = onPrimaryFilterClick,
            label = "Filters"
        )

        val secondaryFilters = state.selectedFilters?.secondaryFilterAttribute
        if (secondaryFilters != null && secondaryFilters.values.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FiltersRow(
                filter = secondaryFilters,
                selectedFilterValue = state.selectedFilters.secondaryFilterValue,
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
            val interactionSource = remember { MutableInteractionSource() }
            val selected = selectedFilterValue == filterValue.value
            val focused by interactionSource.collectIsFocusedAsState()
            if (filter.style == SearchFilter.Style.IMAGE && filterValue.imageUrl != null) {
                Surface(
                    onClick = { onClick(filterValue) },
                    interactionSource = interactionSource,
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    ),
                ) {
                    AsyncImage(
                        model = filterValue.imageUrl,
                        contentDescription = filterValue.value,
                        placeholder = previewPlaceholder(),
                        modifier = Modifier
                            .alpha(if (selected || focused) 1f else 0.3f)
                            .size(56.dp)
                    )
                }
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

@Composable
private fun Header(
    state: PropertySearchViewModel.State,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    Row(Modifier.padding(Overscan.defaultPadding(excludeBottom = true))) {
        AsyncImage(
            model = state.headerLogo,
            contentDescription = "Logo",
            placeholder = previewPlaceholder(),
            modifier = Modifier
                .height(48.dp)
                .widthIn(max = 80.dp)
        )
        Spacer(Modifier.width(24.dp))
        Column {
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
}

@Composable
private fun previewPlaceholder(@DrawableRes id: Int = R.drawable.elv_logo_bw): Painter? {
    return if (LocalInspectionMode.current) {
        painterResource(id = id)
    } else {
        null
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
            )
        ),
        query = "",
        onPrimaryFilterClick = {},
        onSecondaryFilterClick = {},
        onQueryChanged = {},
        onSearchClicked = {},
    )
}
