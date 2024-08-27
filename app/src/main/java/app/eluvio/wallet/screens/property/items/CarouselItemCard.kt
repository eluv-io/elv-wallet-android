package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.common.defaultMediaItemClickHandler
import app.eluvio.wallet.screens.destinations.MediaGridDestination
import app.eluvio.wallet.screens.destinations.UpcomingVideoDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.disabledItemAlpha
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.rememberToaster

@Composable
fun CarouselItemCard(carouselItem: CarouselItem, cardHeight: Dp, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val toaster = rememberToaster()
    when (carouselItem) {
        is CarouselItem.Media -> Column(modifier = modifier.width(IntrinsicSize.Min)) {
            val entity = carouselItem.entity
            MediaItemCard(
                entity,
                cardHeight = cardHeight,
                onMediaItemClick = { media ->
                    when {
                        media.showAlternatePage -> {
                            toaster.toast("TODO: showAlternatePage: ${entity.resolvedPermissions?.alternatePageId}")
                        }

                        media.showPurchaseOptions -> {
                            toaster.toast("TODO: showPurchaseOptions")
                        }

                        media.resolvedPermissions?.authorized == false -> {
                            // TODO: This shouldn't really happen, just for dev purposes
                            toaster.toast("You are not authorized to view this content (behavior: ${entity.resolvedPermissions?.behavior})")
                        }

                        media.mediaItemsIds.isNotEmpty() -> {
                            // This media item is a container for other media (e.g. a media list/collection)
                            navigator(
                                MediaGridDestination(
                                    propertyId = carouselItem.propertyId,
                                    mediaContainerId = media.id
                                ).asPush()
                            )
                        }

                        media.liveVideoInfo?.started == false -> {
                            // this is a live video that hasn't started yet.
                            navigator(
                                UpcomingVideoDestination(
                                    propertyId = carouselItem.propertyId,
                                    mediaItemId = media.id,
                                ).asPush()
                            )
                        }

                        else -> {
                            defaultMediaItemClickHandler(navigator).invoke(media)
                        }
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                entity.name,
                style = MaterialTheme.typography.label_24.copy(fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.thenIf(entity.isDisabled) {
                    alpha(MaterialTheme.colorScheme.disabledItemAlpha)
                }
            )
        }

        is CarouselItem.RedeemableOffer -> OfferCard(
            carouselItem,
            cardHeight
        )

        is CarouselItem.SubpropertyLink -> SubpropertyCard(
            carouselItem,
            cardHeight
        )

        is CarouselItem.CustomCard -> {
            CustomCard(carouselItem, cardHeight, modifier)
        }

        is CarouselItem.ItemPurchase -> ItemPurchaseCard(
            item = carouselItem,
            cardHeight = cardHeight
        )
    }
}

@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun CarouselItemCardPreview() = EluvioThemePreview {
    CarouselItemCard(
        carouselItem = CarouselItem.Media(
            entity = MediaEntity().apply {
                name = "this is a very very very very long title"
            },
            propertyId = "property"
        ), 120.dp
    )
}
