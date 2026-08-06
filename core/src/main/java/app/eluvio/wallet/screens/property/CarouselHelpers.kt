package app.eluvio.wallet.screens.property

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.HeroActionEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.SectionItemEntity
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.HeroAction
import app.eluvio.wallet.screens.qrdialogs.generic.FullscreenQRDialogNavArgs
import app.eluvio.wallet.data.GridContentOverride
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.toHtmlAnnotated
import kotlinx.collections.immutable.toImmutableList
import app.eluvio.wallet.screens.property.mediagrid.MediaGridNavArgs

/**
 * The maximum number of items to display in a carousel before showing a "View All" button for "carousel" sections.
 */
private const val VIEW_ALL_THRESHOLD_CAROUSEL = 5

/**
 * The maximum number of items to display in a grid before showing a "View All" button for "grid" sections.
 * This is set to a high value to avoid showing the "View All" button in most cases.
 */
private const val VIEW_ALL_THRESHOLD_GRID = 1000

/** Corner radius to use for hero CTA buttons that don't define one. */
private const val DEFAULT_ACTION_CORNER_RADIUS = 5

/**
 * Converts a [MediaPageSectionEntity] to a list of [DynamicPageLayoutState.Section]s.
 * Usually this will be a single section, but in the case of a hero section, it may be multiple.
 */
fun MediaPageSectionEntity.toDynamicSections(
    parentPermissionContext: PermissionContext,
    playbackStore: PlaybackStore,
    filters: PropertySearchFiltersEntity? = null,
    // Hero actions only reference their target media by id, so the media items they point to have
    // to be fetched separately and handed to us here (keyed by media id).
    heroActionMedia: Map<String, MediaEntity> = emptyMap(),
    /**
     * Resolves the card theme for a given Section. Takes a Section rather than a theme, because
     * sub-sections of a "container" can each override the theme.
     */
    cardThemeResolver: (MediaPageSectionEntity) -> CardThemeEntity? = { null },
): List<DynamicPageLayoutState.Section> {
    return when (type) {
        MediaPageSectionEntity.TYPE_AUTOMATIC,
        MediaPageSectionEntity.TYPE_MANUAL,
        MediaPageSectionEntity.TYPE_SEARCH -> listOf(
            this.toCarouselSection(
                parentPermissionContext,
                filters,
                playbackStore,
                cardThemeResolver(this)
            )
        )

        MediaPageSectionEntity.TYPE_HERO -> this.toHeroSections(
            parentPermissionContext,
            heroActionMedia
        )
        MediaPageSectionEntity.TYPE_CONTAINER -> {
            // Create a title row if it exists
            val titleRow = listOfNotNull(displaySettings?.title?.let {
                DynamicPageLayoutState.Section.SectionHeader(
                    id,
                    "\n${it}".toHtmlAnnotated(),
                )
            })
            // For now, just swap out container sections with their sub-sections.
            // In the future we'll want to add proper support for containers with filtering.
            titleRow + subSections.flatMap {
                it.toDynamicSections(
                    parentPermissionContext,
                    playbackStore,
                    filters,
                    heroActionMedia,
                    cardThemeResolver
                )
            }
        }

        else -> emptyList()
    }
}

private fun MediaPageSectionEntity.toCarouselSection(
    parentPermissionContext: PermissionContext,
    filters: PropertySearchFiltersEntity? = null,
    playbackStore: PlaybackStore,
    cardTheme: CardThemeEntity?,
): DynamicPageLayoutState.Section.Carousel {
    val permissionContext = parentPermissionContext.copy(sectionId = id)
    val items = items.toCarouselItems(permissionContext, displaySettings, playbackStore)
    val displayLimit = displaySettings?.displayLimit?.takeIf { it > 0 } ?: items.size
    val viewAllThreshold = if (displaySettings?.displayFormat == DisplayFormat.GRID) {
        VIEW_ALL_THRESHOLD_GRID
    } else {
        VIEW_ALL_THRESHOLD_CAROUSEL
    }
    val showViewAll = items.size > displayLimit || items.size > viewAllThreshold
    val filterAttribute = filters?.attributes?.get(primaryFilter)

    val gridContentOverride = this.items
        .takeIf { type == MediaPageSectionEntity.TYPE_SEARCH }
        ?.mapNotNull { it.media?.id }
        ?.let { mediaItemIds ->
            GridContentOverride(
                title = displaySettings?.title ?: "",
                mediaItemsOverride = mediaItemIds
            )
        }

    return DynamicPageLayoutState.Section.Carousel(
        permissionContext,
        displaySettings = displaySettings,
        items = items.take(displayLimit).toImmutableList(),
        filterAttribute = filterAttribute,
        viewAllNavigationEvent = MediaGridNavArgs(permissionContext, gridContentOverride)
            .takeIf { showViewAll }
            ?.asPush(),
        cardTheme = cardTheme,
    )
}

private fun MediaPageSectionEntity.toHeroSections(
    parentPermissionContext: PermissionContext,
    heroActionMedia: Map<String, MediaEntity>,
): List<DynamicPageLayoutState.Section> {
    val permissionContext = parentPermissionContext.copy(sectionId = id)
    return items.flatMap { item ->
        val sectionIdPrefix = "${this.id}-${item.id}"
        listOfNotNull(
            item.displaySettings?.logoUrl?.let {
                DynamicPageLayoutState.Section.Banner("${sectionIdPrefix}-banner", it)
            },
            item.displaySettings?.title?.ifEmpty { null }?.let {
                DynamicPageLayoutState.Section.Title(
                    sectionId = "$sectionIdPrefix-title",
                    text = it.toHtmlAnnotated()
                )
            },
            item.displaySettings?.description?.ifEmpty { null }?.let {
                DynamicPageLayoutState.Section.Description(
                    sectionId = "$sectionIdPrefix-description",
                    text = it.toHtmlAnnotated()
                )
            },
            item.actions
                .mapNotNull { it.toHeroAction(permissionContext, heroActionMedia) }
                .takeIf { it.isNotEmpty() }
                ?.let {
                    DynamicPageLayoutState.Section.HeroActions(
                        sectionId = "$sectionIdPrefix-actions",
                        actions = it.toImmutableList()
                    )
                }
        )
    }
}

/**
 * Returns null when we can't figure out where this action should take us, in which case we'd
 * rather not show a button at all. Note that this includes the case where the action's media item
 * hasn't been fetched yet - the button will show up once it is.
 */
private fun HeroActionEntity.toHeroAction(
    permissionContext: PermissionContext,
    heroActionMedia: Map<String, MediaEntity>,
): HeroAction? {
    val navigationEvent = when (behavior) {
        HeroActionEntity.BEHAVIOR_MEDIA_LINK -> {
            // Delegate to the media item, so live/upcoming/unauthorized media are handled the
            // same way they are when clicked from a carousel.
            heroActionMedia[mediaId]
                ?.let { media -> media.onClickTarget(permissionContext.copy(mediaItemId = media.id)) }
        }

        // Hero page links are always to a page within the current property.
        HeroActionEntity.BEHAVIOR_PAGE_LINK -> pageId
            ?.let { PropertyDetailNavArgs(propertyId = permissionContext.propertyId, pageId = it) }

        HeroActionEntity.BEHAVIOR_EXTERNAL_LINK -> url
            ?.let {
                FullscreenQRDialogNavArgs(
                    url = it,
                    title = "Point your camera to the QR Code below for content"
                )
            }

        else -> null
    }?.asPush()
    val buttonText = text
    if (navigationEvent == null || buttonText == null) {
        Log.w("Ignoring hero action with no click target: $this")
        return null
    }
    return HeroAction(
        id = id,
        text = buttonText,
        backgroundColor = backgroundColor.toColorOrNull() ?: Color.White,
        textColor = textColor.toColorOrNull() ?: Color.Black,
        borderColor = borderColor.toColorOrNull(),
        cornerRadius = (borderRadius ?: DEFAULT_ACTION_CORNER_RADIUS).dp,
        navigationEvent = navigationEvent,
    )
}

private fun String?.toColorOrNull(): Color? {
    val hex = this ?: return null
    return runCatching { Color(hex.toColorInt()) }
        .onFailure { Log.w("Failed to parse color: $hex") }
        .getOrNull()
}

fun List<SectionItemEntity>.toCarouselItems(
    parentPermissionContext: PermissionContext,
    sectionDisplaySettings: DisplaySettings?,
    playbackStore: PlaybackStore,
): List<CarouselItem> {
    return mapNotNull { item ->
        val bannerImage = item.bannerImageUrl
        val isBannerSection = sectionDisplaySettings?.displayFormat == DisplayFormat.BANNER
        if (isBannerSection && bannerImage == null) {
            Log.w("Section item inside a Banner section, doesn't have a banner image configured")
            return@mapNotNull null
        }
        val permissionContext = parentPermissionContext.copy(sectionItemId = item.id)
        val result = when {
            // Filter out hidden items
            item.isHidden || item.media?.isHidden == true -> null

            item.linkData?.externalLink != null -> item.linkData?.externalLink?.let {
                CarouselItem.ExternalLink(
                    permissionContext = permissionContext,
                    url = it,
                    displaySettings = item.displaySettings,
                    forceDisabled = item.disabled,
                )
            }

            item.linkData != null -> {
                CarouselItem.PageLink(
                    permissionContext = permissionContext,
                    // If linkData doesn't have a propertyId,
                    // assume this is a link to page within the current property.
                    propertyId = item.linkData?.linkPropertyId ?: permissionContext.propertyId,
                    pageId = item.linkData?.linkPageId,
                    displaySettings = item.displaySettings,
                    forceDisabled = item.disabled,
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
                    displayOverrides = displayOverrides,
                    playbackProgress = playbackStore.getPlaybackProgress(item.media!!.id),
                    forceDisabled = item.disabled,
                )
            }

            item.isPurchaseItem -> {
                CarouselItem.ItemPurchase(
                    permissionContext = permissionContext,
                    displaySettings = item.displaySettings,
                    forceDisabled = item.disabled,
                )
            }

            else -> CarouselItem.VisualOnly(
                permissionContext = permissionContext,
                displaySettings = item.displaySettings,
                forceDisabled = item.disabled,
            )
        }

        // Wrap in a banner if necessary, otherwise return as-is
        if (result != null && isBannerSection && bannerImage != null) {
            result.asBanner(bannerImage, sectionDisplaySettings?.fullBleed == true)
        } else {
            result
        }
    }
}
