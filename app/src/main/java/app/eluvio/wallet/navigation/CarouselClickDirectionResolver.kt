package app.eluvio.wallet.navigation

import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.destinations.PurchasePromptDestination
import app.eluvio.wallet.screens.destinations.RedeemDialogDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import com.ramcosta.composedestinations.spec.Direction

/**
 * Returns the [Direction] to navigate to when a carousel item is clicked, or null if no navigation.
 */
fun CarouselItem.onClickDirection(): Direction? {
    return when (this) {
        is CarouselItem.Media -> {
            // Delegate to the media entity
            entity.onClickDirection(permissionContext)
        }

        is CarouselItem.BannerWrapper -> {
            // BannerWrapper is just used to change how the item is displayed, but the click action
            // is still the same as the original item.
            delegate.onClickDirection()
        }

        is CarouselItem.ItemPurchase -> PurchasePromptDestination(permissionContext)
        is CarouselItem.PageLink -> PropertyDetailDestination(
            propertyId = propertyId,
            pageId = pageId
        )

        is CarouselItem.RedeemableOffer -> RedeemDialogDestination(
            contractAddress = contractAddress,
            tokenId = tokenId,
            offerId = offerId
        )
    }
}
