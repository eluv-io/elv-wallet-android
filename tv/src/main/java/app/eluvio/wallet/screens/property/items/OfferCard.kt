package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.WrapContentText
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.theme.onRedeemTagSurface
import app.eluvio.wallet.theme.redeemTagSurface

@Composable
fun OfferCard(
    item: DynamicPageLayoutState.CarouselItem.RedeemableOffer,
    cardHeight: Dp,
    onClick: () -> Unit
) {
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
        onClick = onClick,
        modifier = Modifier.size(cardHeight),
        focusedOverlay = {
            offerTitle()
            rewardOverlay()
        },
        unFocusedOverlay = rewardOverlay
    )
}
