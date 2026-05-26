package app.eluvio.wallet.navigation

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import app.eluvio.wallet.screens.qrdialogs.generic.FullscreenQRDialogNavArgs
import app.eluvio.wallet.screens.redeemdialog.RedeemDialogNavArgs

/**
 * Returns the route to navigate to when a carousel item is clicked, or null if no navigation.
 */
fun CarouselItem.onClickTarget(): NavKey? {
    if (forceDisabled) {
        return null
    }
    return when (this) {
        is CarouselItem.Media -> {
            // Delegate to the media entity
            entity.onClickTarget(permissionContext)
        }

        is CarouselItem.BannerWrapper -> {
            // BannerWrapper is just used to change how the item is displayed, but the click action
            // is still the same as the original item.
            delegate.onClickTarget()
        }

        is CarouselItem.ItemPurchase -> PurchasePromptNavArgs(permissionContext)
        is CarouselItem.PageLink -> PropertyDetailNavArgs(
            propertyId = propertyId,
            pageId = pageId
        )

        is CarouselItem.ExternalLink -> FullscreenQRDialogNavArgs(
            url = url,
            title = "Point your camera to the QR Code below for content"
        )

        is CarouselItem.RedeemableOffer -> RedeemDialogNavArgs(
            contractAddress = contractAddress,
            tokenId = tokenId,
            offerId = offerId
        )

        // VisualOnly items don't react to clicks
        is CarouselItem.VisualOnly -> null
    }
}
