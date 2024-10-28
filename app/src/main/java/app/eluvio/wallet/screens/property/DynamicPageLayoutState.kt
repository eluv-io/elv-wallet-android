package app.eluvio.wallet.screens.property

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import kotlinx.parcelize.Parcelize


/**
 * Currently this is only used by PropertyPages, but we were planning to use it as the new
 * NFTDetail page, so we made it more generic so different ViewModels can provide dynamic layouts.
 */
@Immutable
data class DynamicPageLayoutState(
    val backgroundImageUrl: String? = null,
    val backgroundVideo: MediaSource? = null,
    val sections: List<Section> = emptyList(),

    val searchNavigationEvent: NavigationEvent? = null,
    val propertyLinks: List<PropertyLink> = emptyList(),

    // For cross-app deeplinks
    val backLinkUrl: String? = null,
    val backButtonLogo: String? = null,
) {
    fun isEmpty() = sections.isEmpty() && backgroundImageUrl == null

    sealed interface Section {
        val sectionId: String

        // TODO: maybe combine Title and Description into a single "Text" Row type,
        //  but then we'd have to start passing around predefined styles or something
        @Immutable
        data class Title(override val sectionId: String, val text: AnnotatedString) : Section

        @Immutable
        data class Description(override val sectionId: String, val text: AnnotatedString) : Section

        // Note: This has nothing to do with BannerWrapper and we should probably just remove this
        // section type.
        @Immutable
        data class Banner(override val sectionId: String, val imageUrl: String) : Section

        @Immutable
        data class Carousel(
            val permissionContext: PermissionContext,
            val displaySettings: DisplaySettings? = null,
            val viewAllNavigationEvent: NavigationEvent? = null,
            val items: List<CarouselItem>,
            val filterAttribute: FilterAttributeEntity? = null,
        ) : Section {
            override val sectionId: String =
                requireNotNull(permissionContext.sectionId) { "PermissionContext.sectionId is null" }
        }
    }

    sealed interface CarouselItem {
        val permissionContext: PermissionContext

        @Immutable
        data class Media(
            override val permissionContext: PermissionContext,
            val entity: MediaEntity,
            val displayOverrides: DisplaySettings? = null,
        ) : CarouselItem

        @Immutable
        data class PageLink(
            override val permissionContext: PermissionContext,
            // Property ID to link to
            val propertyId: String,
            // Page ID to link to
            val pageId: String?,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        @Immutable
        data class ExternalLink(
            override val permissionContext: PermissionContext,
            val url: String,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        @Immutable
        data class RedeemableOffer(
            override val permissionContext: PermissionContext,
            val offerId: String,
            val name: String,
            val fulfillmentState: RedeemableOfferEntity.FulfillmentState,
            val contractAddress: String,
            val tokenId: String,
            val imageUrl: String?,
            val animation: MediaSource?,
        ) : CarouselItem

        @Immutable
        data class ItemPurchase(
            override val permissionContext: PermissionContext,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        /**
         * Any type of item can appear inside a section with display_type="banner".
         * In that case it will (should) have a "banner_image" defined and we'll display that
         * instead of the item's "normal" UI. However the onClick behavior still works the same, so
         * instead of this being a standalone SectionItem type, it wraps the "real" item, which
         * we'll use for the onClick behavior.
         */
        @Immutable
        data class BannerWrapper(
            val delegate: CarouselItem,
            val bannerImageUrl: String
        ) : CarouselItem by delegate
    }

    @Parcelize
    @Immutable
    data class PropertyLink(
        val id: String,
        val name: String,
        val isCurrent: Boolean,
    ) : Parcelable
}

/**
 * Convenience method to "convert" any CarouselItem to look like a banner.
 */
fun CarouselItem.asBanner(bannerImageUrl: String): CarouselItem {
    return when (this) {
        is CarouselItem.BannerWrapper -> copy(bannerImageUrl = bannerImageUrl)
        else -> CarouselItem.BannerWrapper(this, bannerImageUrl)
    }
}
