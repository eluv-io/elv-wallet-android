package app.eluvio.wallet.navigation

import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem

/**
 * Returns the [NavTarget] to navigate to when a carousel item is clicked, or null if no navigation.
 */
fun CarouselItem.onClickTarget(): NavTarget? {
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

        is CarouselItem.ItemPurchase -> NavTarget.PurchasePrompt(permissionContext)
        is CarouselItem.PageLink -> NavTarget.PropertyDetail(
            propertyId = propertyId,
            pageId = pageId
        )

        is CarouselItem.ExternalLink -> NavTarget.FullscreenQRDialog(
            url = url,
            title = "Point your camera to the QR Code below for content"
        )

        is CarouselItem.RedeemableOffer -> NavTarget.RedeemDialog(
            contractAddress = contractAddress,
            tokenId = tokenId,
            offerId = offerId
        )

        // VisualOnly items don't react to clicks
        is CarouselItem.VisualOnly -> null
    }
}
