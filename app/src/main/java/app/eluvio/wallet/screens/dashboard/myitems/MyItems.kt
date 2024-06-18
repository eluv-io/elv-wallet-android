package app.eluvio.wallet.screens.dashboard.myitems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.navigation.DashboardTabsGraph
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.destinations.NftDetailDestination
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.compose.rememberToaster
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@DashboardTabsGraph
@Destination
@Composable
fun MyItems() {
    val toaster = rememberToaster()
    hiltViewModel<MyItemsViewModel>().subscribeToState(
        onEvent = {
            when (it) {
                is Events.NetworkError -> toaster.toast(it.defaultMessage)
                else -> {}
            }
        },
        onState = { _, state ->
            MyItems(state)
        }
    )
}

@Composable
private fun MyItems(state: AllMediaProvider.State) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.loading) {
            EluvioLoadingSpinner()
        } else if (state.media.isEmpty()) {
            Text(stringResource(R.string.no_content_warning))
        } else {
            val navigator = LocalNavigator.current
            val toaster = rememberToaster()
            MyItemsGrid(state.media, onItemClick = {
                if (it.propertyId != null) {
                    navigator(PropertyDetailDestination(it.propertyId).asPush())
                } else if (it.tokenId == null) {
                    toaster.toast("NFT Packs not supported yet")
                } else {
                    navigator(NftDetailDestination(it.contractAddress, it.tokenId).asPush())
                }
            })
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.MyItemsGrid(
    media: List<AllMediaProvider.Media>,
    onItemClick: (AllMediaProvider.Media) -> Unit
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
    val scrollState = rememberTvLazyGridState()
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }
    TvLazyVerticalGrid(
        state = scrollState,
        columns = TvGridCells.Fixed(columnCount),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalArrangement = Arrangement.spacedBy(cardSpacing),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pivotOffsets = PivotOffsets(0.1f),
        modifier = Modifier
            .focusRestorer(firstItemFocusRequester)
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (it.isKeyUpOf(Key.Back)) {
                    scope.launch {
                        scrollState.animateScrollToItem(0)
                    }
                }
                false
            }
    ) {
        item(span = { TvGridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(10.dp))
        }
        itemsIndexed(media, key = { _, it -> it.key }) { index, mediaItem ->
            MediaCard(
                mediaItem,
                onClick = { onItemClick(mediaItem) },
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstItemFocusRequester)
                }
            )
        }
        item(span = { TvGridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun MyItemsPreview() = EluvioThemePreview {
    val items = listOf(
        AllMediaProvider.Media(
            "key",
            "contract_address",
            "https://x",
            "Single Token",
            "Special Edition",
            "desc",
            "1",
            1
        ),
        AllMediaProvider.Media(
            "key",
            "contract_address",
            "https://x",
            "Token Pack",
            "Pleab Edition",
            "desc",
            null,
            53
        )
    )
    MyItems(AllMediaProvider.State(
        loading = false,
        // create 10 copies of the original list
        (1..10).flatMap { items }.map { it.copy(key = UUID.randomUUID().toString()) }
    ))
}

@Composable
@Preview(device = Devices.TV_720p)
private fun MyItemsPreviewLoading() = EluvioThemePreview {
    MyItems(AllMediaProvider.State(loading = true))
}
