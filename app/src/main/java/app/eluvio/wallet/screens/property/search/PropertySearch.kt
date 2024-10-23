package app.eluvio.wallet.screens.property.search

import androidx.activity.compose.BackHandler
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.v2.SearchFilterAttribute
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
    onPrimaryFilterClick: (SearchFilterAttribute.Value) -> Unit,
    onSecondaryFilterClick: (SearchFilterAttribute.Value) -> Unit,
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
    onPrimaryFilterClick: (SearchFilterAttribute.Value) -> Unit,
    onSecondaryFilterClick: (SearchFilterAttribute.Value) -> Unit
) {
    if (state.primaryFilterValues.isEmpty()) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        FiltersRow(
            filters = state.primaryFilterValues,
            selectedFilterValue = state.selectedFilters?.primaryFilterValue,
            onClick = onPrimaryFilterClick
        )

        val secondaryFilters = state.selectedFilters?.secondaryFilterAttribute?.values.orEmpty()
        if (secondaryFilters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FiltersRow(
                filters = secondaryFilters,
                selectedFilterValue = state.selectedFilters?.secondaryFilterValue,
                onClick = onSecondaryFilterClick,
                labelAlpha = 0f
            )
        }
    }
}

@Composable
private fun FiltersRow(
    filters: List<SearchFilterAttribute.Value>,
    selectedFilterValue: String?,
    onClick: (SearchFilterAttribute.Value) -> Unit,
    // For the secondary row, we still want to take up that space, but not actually show anything
    labelAlpha: Float = 1f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.width(Overscan.horizontalPadding))
        Text("Filters", style = MaterialTheme.typography.carousel_36.withAlpha(labelAlpha))
        filters.forEach { filter ->
            Spacer(Modifier.width(16.dp))
            SearchFilterChip(
                title = filter.value,
                value = filter,
                selected = selectedFilterValue == filter.value,
                onClick = onClick,
                onFocus = {})
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
            primaryFilterValues = List(4) {
                SearchFilterAttribute.Value.from("Primary Filter Value $it")
            },
        ),
        query = "",
        onPrimaryFilterClick = {},
        onSecondaryFilterClick = {},
        onQueryChanged = {},
        onSearchClicked = {},
    )
}
