package app.eluvio.wallet.screens.property.mediagrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.items.CarouselItemCard
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@MainGraph
@Destination(navArgsDelegate = MediaGridNavArgs::class)
@Composable
fun MediaGrid() {
    hiltViewModel<MediaGridViewModel>().subscribeToState { vm, state ->
        if (state.loading) {
            DelayedFullscreenLoader()
        } else {
            MediaGrid(state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaGrid(state: MediaGridViewModel.State) {
    BoxWithConstraints {
        // Space between cards
        val horizCardSpacing = 20.dp
        // Padding on each side of the container
        val horizPadding = Overscan.horizontalPadding
        val cardHeight = remember(maxWidth) {
            calcCardHeight(
                maxWidth,
                minColumnCount = 4,
                horizCardSpacing,
                horizPadding
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(horizCardSpacing),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF16151F), Color(0xFF0C0C10))))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizPadding, vertical = Overscan.verticalPadding)
        ) {
            state.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body_32,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.items.forEach { item ->
                CarouselItemCard(
                    carouselItem = item,
                    cardHeight = cardHeight,
                )
            }
        }
    }
}

/**
 * Calculates the max height we can use, to still be able to fit [minColumnCount] cards in a single
 * row, even if they all have [widestAspectRatio].
 */
private fun calcCardHeight(
    // Width of the container
    totalWidth: Dp,
    // How many "widest" cards should fit in a row
    minColumnCount: Int,
    // Space between cards
    horizCardSpacing: Dp,
    // Padding on each side of the container
    horizPadding: Dp,
    // The widest card we expect to display
    widestAspectRatio: Float = AspectRatio.WIDE,
): Dp {
    val totalPadding = horizPadding * 2
    val totalSpacing = horizCardSpacing * (minColumnCount - 1)
    // Total space left for content after accounting for space/padding
    val availableWidth = totalWidth - totalPadding - totalSpacing
    val cardMaxWidth = availableWidth / minColumnCount

    // Rounded down height for all cards, such that the widest card will still fit
    return (cardMaxWidth / widestAspectRatio).value.toInt().dp
}

@Composable
@Preview(device = Devices.TV_720p)
private fun MediaGridPreview() = EluvioThemePreview {
    MediaGrid(
        MediaGridViewModel.State(
            title = "Grid Title!",
            items = listOf(
                CarouselItem.Media(
                    permissionContext = PermissionContext(propertyId = "property1"),
                    entity = MediaEntity().apply {
                        id = "1"
                        name = "Media 1"
                        mediaType = "image"
                    },
                    playbackProgress = null,
                ),
                CarouselItem.RedeemableOffer(
                    permissionContext = PermissionContext(propertyId = "property1"),
                    offerId = "1",
                    name = "Offer 1",
                    fulfillmentState = RedeemableOfferEntity.FulfillmentState.AVAILABLE,
                    contractAddress = "0x123",
                    tokenId = "1",
                    imageUrl = "https://via.placeholder.com/150",
                    animation = null
                )
            )
        )
    )
}
