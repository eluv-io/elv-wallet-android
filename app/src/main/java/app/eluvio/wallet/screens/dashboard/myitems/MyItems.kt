package app.eluvio.wallet.screens.dashboard.myitems

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.SearchBox
import app.eluvio.wallet.screens.common.SearchFilterChip
import app.eluvio.wallet.screens.destinations.LegacyNftDetailDestination
import app.eluvio.wallet.screens.destinations.NftDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.rememberToaster
import app.eluvio.wallet.util.subscribeToState
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun MyItems() {
    hiltViewModel<MyItemsViewModel>().subscribeToState { vm, state ->
        MyItems(state, vm::onPropertySelected, vm::onQueryChanged)
    }
}

@Composable
private fun MyItems(
    state: MyItemsViewModel.State,
    onPropertySelected: (MyItemsViewModel.State.PropertyInfo?) -> Unit,
    onQueryChanged: (String) -> Unit
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val navigator = LocalNavigator.current
        val toaster = rememberToaster()
        MyItemsGrid(
            state,
            onItemClick = {
                if (it.tokenId == null) {
                    toaster.toast("NFT Packs not supported yet")
                } else if (it.propertyId != null) {
                    navigator(
                        NftDetailDestination(
                            contractAddress = it.contractAddress,
                            tokenId = it.tokenId,
                        ).asPush()
                    )
                } else {
                    navigator(
                        LegacyNftDetailDestination(
                            contractAddress = it.contractAddress,
                            tokenId = it.tokenId,
                        ).asPush()
                    )
                }
            },
            onPropertySelected = onPropertySelected,
            onQueryChanged = onQueryChanged
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.MyItemsGrid(
    state: MyItemsViewModel.State,
    onItemClick: (AllMediaProvider.Media) -> Unit,
    onPropertySelected: (MyItemsViewModel.State.PropertyInfo?) -> Unit,
    onQueryChanged: (String) -> Unit
) {
    val width by rememberUpdatedState(maxWidth)
    val horizontalPadding = 100.dp
    val cardSpacing = 20.dp
    val desiredCardWidth = 240.dp
    val columnCount by remember {
        derivedStateOf {
            val availableWidth = width - horizontalPadding
            val cardWidth = desiredCardWidth + cardSpacing
            (availableWidth / cardWidth).roundToInt()
        }
    }
    val scrollState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val searchBoxFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
        onQueryChanged("")
    }

    LazyVerticalGrid(
        state = scrollState,
        columns = GridCells.Fixed(columnCount),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalArrangement = Arrangement.spacedBy(cardSpacing),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        modifier = Modifier
            .focusRestorer(searchBoxFocusRequester)
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (it.isKeyUpOf(Key.Back) && scrollState.firstVisibleItemIndex > 0) {
                    scope.launch {
                        scrollState.animateScrollToItem(0)
                        searchBoxFocusRequester.requestFocus()
                    }
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "spacer") {
            Spacer(Modifier.height(10.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "search") {
            Column {
                SearchBox(
                    query = searchQuery,
                    hint = "Search My Items",
                    onQueryChanged = {
                        searchQuery = it
                        onQueryChanged(it)
                    },
                    onSearchClicked = {},
                    focusRequester = searchBoxFocusRequester
                )
                Spacer(Modifier.height(2.dp))
                HorizontalDivider()
                PropertyFilterRow(state, onPropertySelected)
            }
        }
        if (state.allMedia.loading) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = { "spinner" }) {
                EluvioLoadingSpinner()
            }
        } else if (state.allMedia.media.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "label") {
                Text(stringResource(R.string.no_content_warning))
            }
        } else {
            items(
                state.allMedia.media,
                key = { mediaItem -> mediaItem.key },
                contentType = { "media" }
            ) { mediaItem ->
                MediaCard(
                    mediaItem,
                    onClick = { onItemClick(mediaItem) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "spacer") {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PropertyFilterRow(
    state: MyItemsViewModel.State,
    onPropertySelected: (MyItemsViewModel.State.PropertyInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state.selectedProperty != null) {
        onPropertySelected(null)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = Overscan.horizontalPadding, vertical = 22.dp),
        modifier = Modifier
            .requiredWidth(screenWidth)
            .focusRestorer()
    ) {
        items(state.properties) { it ->
            SearchFilterChip(
                title = it.name,
                value = it,
                selected = it.id == state.selectedProperty?.id,
                onClick = { property -> onPropertySelected(property) },
                onFocus = { /*No Op*/ }
            )
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun MyItemsPreview() = EluvioThemePreview {
    val items = listOf(
        AllMediaProvider.Media(
            key = "key",
            contractAddress = "contract_address",
            imageUrl = "https://x",
            title = "Single Token",
            subtitle = "Special Edition",
            description = "desc",
            tokenId = "1",
            tokenCount = 1
        ),
        AllMediaProvider.Media(
            key = "key",
            contractAddress = "contract_address",
            imageUrl = "https://x",
            title = "Token Pack",
            subtitle = "Pleab Edition",
            description = "desc",
            tokenId = null,
            tokenCount = 53
        )
    )
    MyItems(
        MyItemsViewModel.State(
            AllMediaProvider.State(
                loading = false,
                // create 10 copies of the original list
                (1..10).flatMap { items }.map { it.copy(key = UUID.randomUUID().toString()) }
            ),
            properties = listOf(
                MyItemsViewModel.State.PropertyInfo("Property 1", "id_1"),
                MyItemsViewModel.State.PropertyInfo("Property 2", "_2")
            ),
        ), onPropertySelected = {}, onQueryChanged = {})
}

@Composable
@Preview(device = Devices.TV_720p)
private fun MyItemsPreviewLoading() = EluvioThemePreview {
    MyItems(
        MyItemsViewModel.State(AllMediaProvider.State(loading = true)),
        onPropertySelected = {},
        onQueryChanged = {}
    )
}
