package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.v2.permissions.DtoWithPermissions
import app.eluvio.wallet.network.dto.v2.permissions.PermissionsDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaPageSectionDto(
    val content: List<SectionItemDto>?,
    @field:Json(name = "hero_items")
    val heroItems: List<HeroItemDto>?,
    val description: String?,
    val id: String,
    val type: String,
    val display: DisplaySettingsDto?,
    // TODO: this also has "filter options", needs to be implemented.
    @field:Json(name = "primary_filter")
    val primaryFilter: String?,
    @field:Json(name = "secondary_filter")
    val secondaryFilter: String?,
    override val permissions: PermissionsDto?,

    // Section of type "container" will have this field defined (assuming ?resolve_subsections=true)
    @field:Json(name = "sections_resolved")
    val subSections: List<MediaPageSectionDto>?,
) : DtoWithPermissions

@JsonClass(generateAdapter = true)
data class SectionItemDto(
    val id: String,

    val type: String,
    @field:Json(name = "media_type")
    val mediaType: String?,
    val media: MediaItemV2Dto?,

    @field:Json(name = "use_media_settings")
    val useMediaSettings: Boolean?,

    // Subproperty link data
    @field:Json(name = "subproperty_id")
    val subpropertyId: String?,
    @field:Json(name = "subproperty_page_id")
    val subpropertyPageId: String?,

    // Property link data
    @field:Json(name = "property_id")
    val propertyId: String?,
    @field:Json(name = "property_page_id")
    val propertyPageId: String?,

    // Page link data
    @field:Json(name = "page_id")
    val pageId: String?,

    val display: DisplaySettingsDto?,
    override val permissions: PermissionsDto?,

    // SectionsItems inside a Banner section will have this field defined
    @field:Json(name = "banner_image")
    val bannerImage: AssetLinkDto?,
) : DtoWithPermissions

@JsonClass(generateAdapter = true)
data class HeroItemDto(
    val id: String,
    val display: DisplaySettingsDto?,
)
