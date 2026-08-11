package app.eluvio.wallet.screens.property

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.PropertyLink
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import kotlinx.collections.immutable.ImmutableList

@Immutable
@JvmInline
value class ImmutableMediaSource(val source: MediaSource)

/**
 * Currently this is only used by PropertyPages, but we were planning to use it as the new
 * NFTDetail page, so we made it more generic so different ViewModels can provide dynamic layouts.
 */
@Immutable
data class DynamicPageLayoutState(
    val backgroundImageUrl: FabricUrl? = null,
    val backgroundVideo: ImmutableMediaSource? = null,
    val sections: List<Section> = emptyList(),

    // The property's header logo. Mobile shows this in the toolbar; TV currently ignores it.
    val headerLogo: FabricUrl? = null,

    val searchNavigationEvent: NavigationEvent? = null,

    // Single-property builds have no Dashboard drawer to reach these from, so the Property page
    // hosts them in its action row instead. Null in normal builds (and My Items is also null
    // when the user owns nothing here).
    val profileNavigationEvent: NavigationEvent? = null,
    val myItemsNavigationEvent: NavigationEvent? = null,

    val propertyLinks: List<PropertyLink> = emptyList(),

    // For cross-app deeplinks
    val backLinkUrl: String? = null,
    val backButtonLogo: String? = null,
) {
    fun isEmpty() = sections.isEmpty() && backgroundImageUrl == null

    sealed interface Section {
        val sectionId: String

        // TODO: maybe combine Title/Description/SectionHeader into a single "Text" Row type,
        //  but then we'd have to start passing around a "style" enum.
        @Immutable
        data class Title(override val sectionId: String, val text: AnnotatedString) : Section

        @Immutable
        data class Description(override val sectionId: String, val text: AnnotatedString) : Section

        @Immutable
        data class SectionHeader(
            override val sectionId: String,
            val text: AnnotatedString,
        ) : Section

        // Note: This has nothing to do with BannerWrapper and we should probably just remove this
        // section type.
        @Immutable
        data class Banner(override val sectionId: String, val imageUrl: FabricUrl) : Section

        @Immutable
        data class Carousel(
            val permissionContext: PermissionContext,
            val displaySettings: DisplaySettings? = null,
            val viewAllNavigationEvent: NavigationEvent? = null,
            val items: ImmutableList<CarouselItem>,
            val filterAttribute: FilterAttributeEntity? = null,
        ) : Section {
            override val sectionId: String =
                requireNotNull(permissionContext.sectionId) { "PermissionContext.sectionId is null" }
        }
    }

    @Immutable
    sealed interface CarouselItem {
        val permissionContext: PermissionContext
        val forceDisabled: Boolean

        @Immutable
        data class Media(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
            val entity: MediaEntity,
            val playbackProgress: Float?,
            val displayOverrides: DisplaySettings? = null,
        ) : CarouselItem

        @Immutable
        data class PageLink(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
            // Property ID to link to
            val propertyId: String,
            // Page ID to link to
            val pageId: String?,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        @Immutable
        data class ExternalLink(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
            val url: String,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        @Immutable
        data class RedeemableOffer(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
            val offerId: String,
            val name: String,
            val fulfillmentState: RedeemableOfferEntity.FulfillmentState,
            val contractAddress: String,
            val tokenId: String,
            val imageUrl: FabricUrl?,
            val animation: MediaSource?,
        ) : CarouselItem

        @Immutable
        data class ItemPurchase(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
            val displaySettings: DisplaySettings?,
        ) : CarouselItem

        @Immutable
        data class VisualOnly(
            override val permissionContext: PermissionContext,
            override val forceDisabled: Boolean,
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
            val bannerImageUrl: FabricUrl,
            val fullBleed: Boolean,
        ) : CarouselItem by delegate
    }

}

/**
 * Convenience method to "convert" any CarouselItem to look like a banner.
 */
fun CarouselItem.asBanner(bannerImageUrl: FabricUrl, fullBleed: Boolean): CarouselItem {
    return when (this) {
        is CarouselItem.BannerWrapper -> copy(bannerImageUrl = bannerImageUrl, fullBleed = fullBleed)
        else -> CarouselItem.BannerWrapper(this, bannerImageUrl, fullBleed)
    }
}
