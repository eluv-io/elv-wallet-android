package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.SectionItemEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettingsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toContentPermissionsEntity
import app.eluvio.wallet.network.dto.v2.HeroItemDto
import app.eluvio.wallet.network.dto.v2.MediaPageSectionDto
import app.eluvio.wallet.network.dto.v2.SectionItemDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty

private const val TYPE_PROPERTY_LINK = "property_link"
private const val TYPE_SUBPROPERTY_LINK = "subproperty_link"
private const val TYPE_PAGE_LINK = "page_link"
private const val TYPE_EXTERNAL_LINK = "external_link"

private val supportedSectionItemTypes =
    setOf(
        "media",
        "item_purchase",
        "visual",
        "visual_only",
        TYPE_PROPERTY_LINK,
        TYPE_SUBPROPERTY_LINK,
        TYPE_PAGE_LINK,
        TYPE_EXTERNAL_LINK,
    )

fun MediaPageSectionDto.toEntity(baseUrl: String): MediaPageSectionEntity {
    val dto = this

    return MediaPageSectionEntity().apply {
        id = dto.id
        type = dto.type
        displaySettings = dto.display?.toEntity(baseUrl) ?: DisplaySettingsEntity()
        if (type == MediaPageSectionEntity.TYPE_HERO) {
            items = dto.heroItems?.map { it.toEntity(baseUrl) }.toRealmListOrEmpty()
            // Find some non-null bg image/video from children and apply to self
            items.forEach { sectionItem ->
                sectionItem.displaySettings?.heroBackgroundImageUrl?.let {
                    displaySettings?.heroBackgroundImageUrl = it
                }
                sectionItem.displaySettings?.heroBackgroundVideoHash?.let {
                    displaySettings?.heroBackgroundVideoHash = it
                }
            }
        } else {
            items = dto.content?.mapNotNull { it.toEntity(baseUrl) }.toRealmListOrEmpty()
        }

        primaryFilter = dto.primaryFilter
        secondaryFilter = dto.secondaryFilter

        rawPermissions = dto.permissions?.toContentPermissionsEntity()

        subSections = dto.subSections?.map { it.toEntity(baseUrl) }.toRealmListOrEmpty()
    }
}

private fun SectionItemDto.toEntity(baseUrl: String): SectionItemEntity? {
    val dto = this
    if (dto.type !in supportedSectionItemTypes) return null
    return SectionItemEntity().apply {
        id = dto.id
        mediaType = dto.mediaType
        media = dto.media?.toEntity(baseUrl)
        if (dto.type == "media" && media == null) {
            // This section is supposed to be a media item, but the media is missing. Ignore.
            return null
        }
        useMediaDisplaySettings = dto.useMediaSettings == true
        rawPermissions = dto.permissions?.toContentPermissionsEntity()

        linkData = dto.getLinkDataEntity()

        bannerImageUrl = dto.bannerImage?.toUrl(baseUrl)

        isPurchaseItem = dto.type == "item_purchase"

        displaySettings = dto.display?.toEntity(baseUrl)
    }
}

private fun HeroItemDto.toEntity(baseUrl: String): SectionItemEntity {
    val dto = this
    return SectionItemEntity().apply {
        id = dto.id
        displaySettings = dto.display?.toEntity(baseUrl)
    }
}

/**
 * Returns a [SectionItemEntity.LinkData] object if this SectionItem represents a link.
 * Because the server doesn't clear irrelevant fields, we can't just find the first non-null link
 * field. We have to only look at the fields for the corresponding type.
 */
private fun SectionItemDto.getLinkDataEntity(): SectionItemEntity.LinkData? {
    return when (type) {
        TYPE_PROPERTY_LINK -> {
            SectionItemEntity.LinkData().apply {
                linkPropertyId = propertyId
                linkPageId = propertyPageId
            }
        }

        TYPE_SUBPROPERTY_LINK -> {
            SectionItemEntity.LinkData().apply {
                linkPropertyId = subpropertyId
                linkPageId = subpropertyPageId
            }
        }

        TYPE_PAGE_LINK -> {
            SectionItemEntity.LinkData().apply {
                linkPageId = pageId
            }
        }

        TYPE_EXTERNAL_LINK -> {
            SectionItemEntity.LinkData().apply {
                externalLink = url
            }
        }

        else -> null
    }
}
