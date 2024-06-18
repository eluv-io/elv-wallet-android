package app.eluvio.wallet.screens.dashboard.mymedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.navigation.DashboardTabsGraph
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider
import app.eluvio.wallet.screens.dashboard.myitems.MediaCard
import app.eluvio.wallet.screens.destinations.NftDetailDestination
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.isKeyUpOf
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@DashboardTabsGraph
@Destination
@Composable
fun MyMedia() {
    hiltViewModel<MyMediaViewModel>().subscribeToState { _, state ->
        MyMedia(state)
    }
}

@Composable
private fun MyMedia(state: MyMediaViewModel.State) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.loading) {
            EluvioLoadingSpinner()
        } else if (state.isEmpty()) {
            Text(stringResource(R.string.no_content_warning))
        } else {
            MyMediaGrid(state)
        }
    }
}

@Composable
private fun MyMediaGrid(state: MyMediaViewModel.State) {
    val scrollState = rememberTvLazyListState()
    val scope = rememberCoroutineScope()
    TvLazyColumn(
        state = scrollState,
        verticalArrangement = Arrangement.spacedBy(30.dp),
        pivotOffsets = PivotOffsets(0.1f),
        modifier = Modifier
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
        spacer(height = 10.dp)

        item {
            FeaturedMediaRow(state.featuredMedia, state.baseUrl)
        }
        // TODO add section media
        items(state.nftMedia.entries.toList()) { (displayName, mediaItems) ->
            NftMediaRow(displayName, mediaItems)
        }
        if (state.myItems.isNotEmpty()) {
            item {
                MyItemsRow(state.myItems)
            }
        }

        spacer(height = 7.dp)
    }
}

@Composable
fun FeaturedMediaRow(featuredMedia: List<MediaEntity>, baseUrl: String?) {
    val firstItemFocusRequester = remember { FocusRequester() }
    TvLazyRow(
        horizontalArrangement = itemArrangement,
        modifier = Modifier.focusRestorer(firstItemFocusRequester)
    ) {
        spacer(width = listSpacerSize)
        itemsIndexed(featuredMedia) { index, media ->
            val url = if (media.posterImagePath != null && baseUrl != null) {
                "$baseUrl${media.posterImagePath}"
            } else {
                media.image
            }
            MediaItemCard(
                media = media,
                imageUrl = url,
                shape = RoundedCornerShape(0.dp),
                cardHeight = 300.dp,
                aspectRatio = MediaEntity.ASPECT_RATIO_POSTER,
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstItemFocusRequester)
                })
        }
        spacer(width = listSpacerSize)
    }
}

@Composable
fun NftMediaRow(displayName: String, mediaItems: List<MediaEntity>) {
    RowHeader(displayName)
    Spacer(Modifier.height(15.dp))
    val firstItemFocusRequester = remember { FocusRequester() }
    TvLazyRow(
        horizontalArrangement = itemArrangement,
        modifier = Modifier.focusRestorer(firstItemFocusRequester)
    ) {
        spacer(width = listSpacerSize)
        itemsIndexed(mediaItems) { index, media ->
            MediaItemCard(
                media,
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstItemFocusRequester)
                })
        }
        spacer(width = listSpacerSize)
    }
}

@Composable
private fun MyItemsRow(myItems: List<AllMediaProvider.Media>) {
    val firstItemFocusRequester = remember { FocusRequester() }
    RowHeader(text = "Items")
    Spacer(Modifier.height(15.dp))
    TvLazyRow(
        horizontalArrangement = itemArrangement,
        modifier = Modifier.focusRestorer(firstItemFocusRequester)
    ) {
        spacer(width = listSpacerSize)
        itemsIndexed(myItems) { index, item ->
            val navigator = LocalNavigator.current
            MediaCard(
                media = item,
                onClick = {
                    if (item.propertyId != null) {
                        navigator(PropertyDetailDestination(item.propertyId).asPush())
                    } else if (item.tokenId != null) {
                        navigator(NftDetailDestination(item.contractAddress, item.tokenId).asPush())
                    }
                },
                modifier = Modifier
                    .width(200.dp)
                    .thenIf(index == 0) {
                        focusRequester(firstItemFocusRequester)
                    }
            )
        }
        spacer(width = listSpacerSize)
    }
}

@Composable
private fun RowHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.carousel_36,
        modifier = Modifier.padding(start = listSpacerSize + itemArrangement.spacing)
    )
}

/**
 * How items are spaced in each row.
 */
private val itemArrangement = Arrangement.spacedBy(20.dp)

/**
 * Extra space to add at the start and end of each row.
 * This is added as a list "item" so [itemArrangement] will also apply to it for a total of 48dp at the start and end of the list.
 */
private val listSpacerSize = 28.dp

@Composable
@Preview(device = Devices.TV_720p)
private fun MyMediaPreview() = EluvioThemePreview {
    MyMedia(
        MyMediaViewModel.State(
            loading = false,
            featuredMedia = List(10) { MediaEntity() },
            nftMedia = mapOf(
                "Section Title" to List(10) { MediaEntity() },
                "Section Title2" to List(10) { MediaEntity() },
            ),
            myItems = List(10) { i ->
                AllMediaProvider.Media(
                    "$i",
                    "",
                    "",
                    "Title #$i",
                    "subtitle",
                    ""
                )
            }
        )
    )
}
