package app.eluvio.mobile.screens.property

import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem

/** Image + title pulled from whichever subtype carries them. */
internal data class CardData(
    val image: Any?,
    val title: String,
)

internal fun CarouselItem.toCard(): CardData = when (this) {
    is CarouselItem.Media -> CardData(image = entity.image, title = entity.name)
    is CarouselItem.PageLink -> CardData(
        displaySettings?.thumbnail(),
        displaySettings?.title.orEmpty()
    )

    is CarouselItem.ExternalLink -> CardData(
        displaySettings?.thumbnail(),
        displaySettings?.title.orEmpty()
    )

    is CarouselItem.RedeemableOffer -> CardData(image = imageUrl, title = name)
    is CarouselItem.ItemPurchase -> CardData(
        displaySettings?.thumbnail(),
        displaySettings?.title.orEmpty()
    )

    is CarouselItem.VisualOnly -> CardData(
        displaySettings?.thumbnail(),
        displaySettings?.title.orEmpty()
    )

    is CarouselItem.BannerWrapper -> {
        // BannerWrapper delegates everything else to its inner item; only the image differs.
        CardData(image = bannerImageUrl, title = delegate.toCard().title)
    }
}

internal fun CarouselItem.isVideo(): Boolean = when (this) {
    is CarouselItem.Media -> entity.mediaType == MediaEntity.MEDIA_TYPE_VIDEO
    is CarouselItem.BannerWrapper -> delegate.isVideo()
    else -> false
}

internal fun CarouselItem.needsInaccessibleOverlay(): Boolean = forceDisabled || when (this) {
    is CarouselItem.Media ->
        entity.isDisabled || entity.showPurchaseOptions || entity.showAlternatePage

    is CarouselItem.BannerWrapper -> delegate.needsInaccessibleOverlay()
    else -> false
}

private fun DisplaySettings.thumbnail() = thumbnailUrlAndRatio?.first
