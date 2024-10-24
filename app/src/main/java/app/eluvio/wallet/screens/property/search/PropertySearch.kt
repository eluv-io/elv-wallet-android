package app.eluvio.wallet.screens.property.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.v2.search.SearchFilter
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.SearchBox
import app.eluvio.wallet.screens.common.SearchFilterChip
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.common.withAlpha
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
            onClick = onPrimaryFilterClick
        )

        val secondaryFilters = state.selectedFilters?.secondaryFilterAttribute
        if (secondaryFilters != null && secondaryFilters.values.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FiltersRow(
                filter = secondaryFilters,
                selectedFilterValue = state.selectedFilters.secondaryFilterValue,
                onClick = onSecondaryFilterClick,
                labelAlpha = 0f
            )
        }
    }
}

@Composable
private fun FiltersRow(
    filter: SearchFilter,
    selectedFilterValue: String?,
    onClick: (SearchFilter.Value) -> Unit,
    // For the secondary row, we still want to take up that space, but not actually show anything
    labelAlpha: Float = 1f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.width(Overscan.horizontalPadding))
        Text("Filters", style = MaterialTheme.typography.carousel_36.withAlpha(labelAlpha))
        filter.values.forEach { filterValue ->
            Spacer(Modifier.width(16.dp))
            val selected = selectedFilterValue == filterValue.value
            if (filter.style == SearchFilter.Style.IMAGE && filterValue.imageUrl != null) {
                val border = ClickableSurfaceDefaults.border(
                    border = if (selected) Border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.border)
                    ) else Border.None
                )
                Surface(
                    onClick = { onClick(filterValue) },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    ),
                    border = border,
                ) {
                    AsyncImage(
                        model = filterValue.imageUrl,
                        contentDescription = filterValue.value,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp),
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
        }
        Spacer(Modifier.width(Overscan.horizontalPadding))
    }
}

@Composable
private fun Header(
    state: PropertySearchViewModel.State,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    val placeholder = if (LocalInspectionMode.current) {
        painterResource(id = R.drawable.elv_logo_bw)
    } else {
        null
    }
    Row(Modifier.padding(Overscan.defaultPadding(excludeBottom = true))) {
        AsyncImage(
            model = state.headerLogo,
            contentDescription = "Logo",
            placeholder = placeholder,
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
@Preview(device = Devices.TV_720p)
private fun PropertySearchPreview() = EluvioThemePreview {
    PropertySearch(
        PropertySearchViewModel.State(
            loading = false,
            propertyName = "FlixVerse",
            primaryFilter = SearchFilter(
                "p1", "Primary Filter",
                List(4) {
                    SearchFilter.Value("Primary Filter Value $it")
                },
                style = SearchFilter.Style.TEXT
            ),
        ),
        query = "",
        onPrimaryFilterClick = {},
        onSecondaryFilterClick = {},
        onQueryChanged = {},
        onSearchClicked = {},
    )
}
