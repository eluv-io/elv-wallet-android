package app.eluvio.wallet.screens.property

import androidx.compose.ui.text.AnnotatedString
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.SectionItemEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.destinations.MediaGridDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.mediagrid.GridContentOverride
import app.eluvio.wallet.util.logging.Log

/**
 * The maximum number of items to display in a carousel before showing a "View All" button.
 */
private const val VIEW_ALL_THRESHOLD = 5

/**
 * Converts a [MediaPageSectionEntity] to a list of [DynamicPageLayoutState.Section]s.
 * Usually this will be a single section, but in the case of a hero section, it may be multiple.
 */
fun MediaPageSectionEntity.toDynamicSections(
    parentPermissionContext: PermissionContext,
    filters: PropertySearchFiltersEntity? = null,
): List<DynamicPageLayoutState.Section> {
    return when (type) {
        MediaPageSectionEntity.TYPE_AUTOMATIC,
        MediaPageSectionEntity.TYPE_MANUAL,
        MediaPageSectionEntity.TYPE_SEARCH -> listOf(
            this.toCarouselSection(
                parentPermissionContext,
                filters,
            )
        )

        MediaPageSectionEntity.TYPE_HERO -> this.toHeroSections()
        MediaPageSectionEntity.TYPE_CONTAINER -> {
            // For now, just swap out container sections with their sub-sections.
            // In the future we'll want to add proper support for containers with filtering.
            subSections.flatMap {
                it.toDynamicSections(parentPermissionContext, filters)
            }
        }

        else -> emptyList()
    }
}

private fun MediaPageSectionEntity.toCarouselSection(
    parentPermissionContext: PermissionContext,
    filters: PropertySearchFiltersEntity? = null,
): DynamicPageLayoutState.Section.Carousel {
    val permissionContext = parentPermissionContext.copy(sectionId = id)
    val items = items.toCarouselItems(permissionContext, displaySettings)
    val displayLimit = displaySettings?.displayLimit?.takeIf { it > 0 } ?: items.size
    val showViewAll = items.size > displayLimit || items.size > VIEW_ALL_THRESHOLD
    val filterAttribute = filters?.attributes?.get(primaryFilter)

    val gridContentOverride = this.items
        .takeIf { type == MediaPageSectionEntity.TYPE_SEARCH }
        ?.mapNotNullTo(arrayListOf()) { it.media?.id }
        ?.let { mediaItemIds ->
            GridContentOverride(
                title = displaySettings?.title ?: "",
                mediaItemsOverride = mediaItemIds
            )
        }

    return DynamicPageLayoutState.Section.Carousel(
        permissionContext,
        displaySettings = displaySettings,
        items = items.take(displayLimit),
        filterAttribute = filterAttribute,
        viewAllNavigationEvent = MediaGridDestination(permissionContext, gridContentOverride)
            .takeIf { showViewAll }
            ?.asPush()
    )
}

private fun MediaPageSectionEntity.toHeroSections(): List<DynamicPageLayoutState.Section> {
    return items.flatMap { item ->
        val sectionIdPrefix = "${this.id}-${item.id}"
        listOfNotNull(
            item.displaySettings?.logoUrl?.url?.let {
                DynamicPageLayoutState.Section.Banner("${sectionIdPrefix}-banner", it)
            },
            item.displaySettings?.title?.ifEmpty { null }?.let {
                DynamicPageLayoutState.Section.Title(
                    sectionId = "$sectionIdPrefix-title",
                    text = AnnotatedString(it)
                )
            },
            item.displaySettings?.description?.ifEmpty { null }?.let {
                DynamicPageLayoutState.Section.Description(
                    sectionId = "$sectionIdPrefix-description",
                    text = AnnotatedString(it)
                )
            }
        )
    }
}

fun List<SectionItemEntity>.toCarouselItems(
    parentPermissionContext: PermissionContext,
    sectionDisplaySettings: DisplaySettings?,
): List<CarouselItem> {
    return mapNotNull { item ->
        val bannerImage = item.bannerImageUrl?.url
        val isBannerSection = sectionDisplaySettings?.displayFormat == DisplayFormat.BANNER
        if (isBannerSection && bannerImage == null) {
            Log.w("Section item inside a Banner section, doesn't have a banner image configured")
            return@mapNotNull null
        }
        val permissionContext = parentPermissionContext.copy(sectionItemId = item.id)
        val result = when {
            // Filter out hidden items
            item.isHidden -> null

            item.linkData != null -> {
                CarouselItem.PageLink(
                    permissionContext = permissionContext,
                    // If linkData doesn't have a propertyId,
                    // assume this is a link to page within the current property.
                    propertyId = item.linkData?.linkPropertyId ?: permissionContext.propertyId,
                    pageId = item.linkData?.linkPageId,
                    displaySettings = item.displaySettings,
                )
            }

            item.media != null -> {
                val aspectRatioOverride = sectionDisplaySettings?.forcedAspectRatio
                val displayOverrides = item.displaySettings
                    ?.takeIf { !item.useMediaDisplaySettings }
                    ?.let { SimpleDisplaySettings.from(it, aspectRatioOverride) }
                    ?: SimpleDisplaySettings(forcedAspectRatio = aspectRatioOverride)
                CarouselItem.Media(
                    permissionContext = permissionContext.copy(mediaItemId = item.media!!.id),
                    entity = item.media!!,
                    displayOverrides = displayOverrides
                )
            }

            item.isPurchaseItem -> {
                CarouselItem.ItemPurchase(
                    permissionContext = permissionContext,
                    displaySettings = item.displaySettings,
                )
            }

            else -> null
        }

        // Wrap in a banner if necessary, otherwise return as-is
        if (result != null && isBannerSection && bannerImage != null) {
            result.asBanner(bannerImage)
        } else {
            result
        }
    }
}
