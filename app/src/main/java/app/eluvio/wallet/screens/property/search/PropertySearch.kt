package app.eluvio.wallet.screens.property.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.SearchFilterAttribute
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.SearchBox
import app.eluvio.wallet.screens.common.SearchFilterChip
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.rows.CAROUSEL_CARD_HEIGHT
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
                onPrimaryFilterSelected = vm::onPrimaryFilterSelected,
                onSecondaryFilterClick = vm::onSecondaryFilterClicked,
                onSearchClicked = vm::onSearchClicked,
                onSubpropertySelected = vm::onSubpropertySelected
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
    onPrimaryFilterSelected: (SearchFilterAttribute.Value?) -> Unit,
    onSecondaryFilterClick: (String?) -> Unit,
    onSearchClicked: () -> Unit,
    onSubpropertySelected: (String?) -> Unit
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
        item(contentType = "subproperty_selector") {
            SubpropertiesRow(state, onSubpropertySelected, query.isEmpty())
        }
        spacer(8.dp)
        if (state.selectedFilters != null) {
            item(contentType = "secondary_filter_selector") {
                SecondaryFilterSelector(
                    state,
                    onPrimaryFilterCleared = { onPrimaryFilterSelected(null) },
                    onSecondaryFilterClick = onSecondaryFilterClick
                )
            }
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
            sections(state.allSections)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubpropertiesRow(
    state: PropertySearchViewModel.State,
    onSubpropertySelected: (String?) -> Unit,
    queryIsEmpty: Boolean,
) {
    val hideSubpropertyTilesAndFilters = queryIsEmpty && state.selectedFilters != null
    if (hideSubpropertyTilesAndFilters || state.subproperties.isEmpty()) {
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Overscan.horizontalPadding)
            .padding(top = 16.dp)
    ) {
        if (queryIsEmpty) {
            SubpropertyTiles(state.subproperties)
        } else {
            SubpropertyFilters(state.subproperties, onSubpropertySelected)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.SubpropertyFilters(
    subproperties: List<SelectableSubproperty>,
    onSubpropertySelected: (String?) -> Unit
) {
    Text(
        "Search In: ",
        style = MaterialTheme.typography.carousel_36,
        modifier = Modifier.align(Alignment.CenterVertically)
    )
    subproperties.forEach { subproperty ->
        val bgColor = if (subproperty.selected) Color.White else Color.Black
        Surface(
            onClick = { onSubpropertySelected(subproperty.takeIf { !it.selected }?.id) },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = bgColor,
                focusedContainerColor = bgColor
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color(0xFF5D5D5D))),
                focusedBorder = Border(BorderStroke(2.dp, Color.White)),
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                AsyncImage(
                    model = subproperty.icon, contentDescription = subproperty.title, modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = Color(0xFF212121), shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                )
                Text(text = subproperty.title ?: "")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowRowScope.SubpropertyTiles(subproperties: List<SelectableSubproperty>) {
    val navigator = LocalNavigator.current
    subproperties.forEach { subproperty ->
        ImageCard(
            imageUrl = subproperty.tile?.url,
            contentDescription = subproperty.title,
            onClick = { navigator(PropertyDetailDestination(subproperty.id).asPush()) },
            // Assume subproperty cards are wide
            modifier = Modifier
                .height(CAROUSEL_CARD_HEIGHT)
                .aspectRatio(AspectRatio.WIDE)
        )
    }
}

@Composable
private fun LoadingSpinner(modifier: Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        EluvioLoadingSpinner()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SecondaryFilterSelector(
    state: PropertySearchViewModel.State,
    onPrimaryFilterCleared: () -> Unit,
    onSecondaryFilterClick: (String?) -> Unit,
) {
    val filter = state.selectedFilters ?: return
    val secondaryFilters = filter.secondaryFilterAttribute?.values.orEmpty()
    val filterCount = secondaryFilters.size

    val backButtonFocusRequester = remember { FocusRequester() }
    val filterFocusRequesters = remember(filterCount) {
        List(filterCount) { FocusRequester() }
    }
    // Made non-lazy to make sure the "back" button (primary filter) is always attached so we can
    // always call .requestFocus() without crashing.
    // The number of secondary filters should be low enough that this is not a performance issue.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .horizontalScroll(rememberScrollState())
            // "Manual" focusRestorer.
            // We need to do this because when the secondary filter is cleared (via hardware back
            // button), for a split second there will be 0 items in the search results, which will
            // cause the system to focus on this row and re-select the last-selected secondary
            // filter.
            .focusProperties {
                enter = {
                    val selectedFilter = state.selectedFilters.secondaryFilterValue
                    if (selectedFilter == null) {
                        backButtonFocusRequester
                    } else {
                        secondaryFilters
                            .indexOfFirst { it.value == selectedFilter }
                            .takeIf { it != -1 }
                            ?.let { index ->
                                filterFocusRequesters[index]
                            }
                            ?: FocusRequester.Default
                    }
                }
            }
            .focusGroup()
    ) {
        Spacer(Modifier.width(Overscan.horizontalPadding))

        Image(
            imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color(0xFF939393)),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.size(12.dp, 30.dp)
        )
        TvButton(
            text = filter.primaryFilterValue,
            onClick = onPrimaryFilterCleared,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            modifier = Modifier
                .focusRequester(backButtonFocusRequester)
                .padding(end = 5.dp)
                .onFocusChanged {
                    if (it.hasFocus) {
                        onSecondaryFilterClick(null)
                    }
                }
        )

        secondaryFilters.forEachIndexed { index, attributeValue ->
            SearchFilterChip(
                title = attributeValue.value,
                value = attributeValue.value,
                selected = filter.secondaryFilterValue == attributeValue.value,
                onClicked = onSecondaryFilterClick,
                modifier = Modifier
                    .focusRequester(filterFocusRequesters[index])
                    .padding(horizontal = 5.dp)
            )
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
            modifier = Modifier.height(48.dp)
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
            subproperties = listOf(
                SelectableSubproperty("id1", "Subproperty1", null, null, false),
                SelectableSubproperty("id2", "Sub2", null, null, false),
            ),
            primaryFilters = DynamicPageLayoutState.Section.Carousel(
                permissionContext = PermissionContext(propertyId = "p", sectionId = "4"),
                displaySettings = SimpleDisplaySettings(
                    displayFormat = DisplayFormat.GRID,
                ),
                items = List(4) {
                    DynamicPageLayoutState.CarouselItem.CustomCard(
                        permissionContext = PermissionContext(propertyId = "property1"),
                        title = "Primary Filter Value ${it + 1}",
                        imageUrl = null,
                        aspectRatio = 16f / 9f,
                        onClick = {})
                }
            ),
        ),
        query = "",
        onPrimaryFilterSelected = {},
        onSecondaryFilterClick = {},
        onQueryChanged = {},
        onSearchClicked = {},
        onSubpropertySelected = {},
    )
}
