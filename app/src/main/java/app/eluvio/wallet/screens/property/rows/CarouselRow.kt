package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.WrapContentText
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.destinations.RedeemDialogDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.theme.onRedeemTagSurface
import app.eluvio.wallet.theme.redeemTagSurface
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.compose.thenIf

private val CARD_HEIGHT = 170.dp

@Composable
fun CarouselRow(item: DynamicPageLayoutState.Row.Carousel) {
    if (item.items.isEmpty()) {
        return
    }
    Spacer(modifier = Modifier.height(16.dp))
    item.title?.let {
        Text(
            it,
            style = MaterialTheme.typography.carousel_36,
            modifier = Modifier.padding(horizontal = Overscan.horizontalPadding)
        )
    }
    item.subtitle?.takeIf { it.isNotEmpty() }?.let {
        Text(
            it,
            style = MaterialTheme.typography.body_32,
            modifier = Modifier.padding(horizontal = Overscan.horizontalPadding)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    val firstItemFocusRequester = remember { FocusRequester() }
    TvLazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.focusRestorer(firstItemFocusRequester)
    ) {
        spacer(width = 28.dp)
        itemsIndexed(item.items) { index, item ->
            when (item) {
                is DynamicPageLayoutState.CarouselItem.Media -> MediaItemCard(
                    item.entity,
                    cardHeight = CARD_HEIGHT,
                    modifier = Modifier.thenIf(index == 0) {
                        focusRequester(firstItemFocusRequester)
                    }
                )

                is DynamicPageLayoutState.CarouselItem.RedeemableOffer -> OfferCard(
                    item,
                    modifier = Modifier.thenIf(index == 0) {
                        focusRequester(firstItemFocusRequester)
                    })
            }
        }
        spacer(width = 28.dp)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun OfferCard(
    item: DynamicPageLayoutState.CarouselItem.RedeemableOffer,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    // It's possible to layer this Text on top of the card (with explicit zIndex modifiers, see:
    // https://issuetracker.google.com/issues/291642442), but then it won't scale right when
    // the card is focused.
    // So instead we draw it both in the focused overlay, and unfocused overlay.
    val rewardOverlay = remember<@Composable BoxScope.() -> Unit> {
        {
            val text = when (item.fulfillmentState) {
                RedeemableOfferEntity.FulfillmentState.AVAILABLE, RedeemableOfferEntity.FulfillmentState.UNRELEASED -> "REWARD"
                RedeemableOfferEntity.FulfillmentState.EXPIRED -> "EXPIRED REWARD"
                RedeemableOfferEntity.FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER -> "CLAIMED REWARD"
            }
            Text(
                text = text,
                style = MaterialTheme.typography.label_24,
                color = MaterialTheme.colorScheme.onRedeemTagSurface,
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        MaterialTheme.colorScheme.redeemTagSurface,
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 0.dp)
                    .align(Alignment.BottomCenter)
            )
            when (item.fulfillmentState) {
                RedeemableOfferEntity.FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER,
                RedeemableOfferEntity.FulfillmentState.EXPIRED -> {
                    // Gray out unavailable offers
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                }

                else -> {
                    /* no-op */
                }
            }
        }
    }
    val offerTitle = remember<@Composable BoxScope.() -> Unit> {
        {
            WrapContentText(
                text = item.name,
                style = MaterialTheme.typography.body_32,
                // TODO: get this from theme
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            )
        }
    }
    ImageCard(
        imageUrl = item.imageUrl,
        contentDescription = item.name,
        onClick = {
            navigator(
                RedeemDialogDestination(
                    item.contractAddress,
                    item.tokenId,
                    item.offerId
                ).asPush()
            )
        },
        modifier = modifier.size(CARD_HEIGHT),
        focusedOverlay = {
            offerTitle()
            rewardOverlay()
        },
        unFocusedOverlay = rewardOverlay
    )
}
