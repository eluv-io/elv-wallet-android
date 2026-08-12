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

    val disabled: Boolean?,

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

    // External link data
    val url: String?,
) : DtoWithPermissions

@JsonClass(generateAdapter = true)
data class HeroItemDto(
    val id: String,
    val display: DisplaySettingsDto?,
    /** CTA buttons to display under the hero's text. */
    val actions: List<HeroActionDto>?,
)

@JsonClass(generateAdapter = true)
data class HeroActionDto(
    val id: String,
    /**
     * What happens when the button is clicked. The server also defines "sign_in", "show_purchase"
     * and "video" behaviors, which we don't support (and therefore don't parse) yet.
     */
    val behavior: String?,
    /** Defined for "media_link" actions. */
    @field:Json(name = "media_id")
    val mediaId: String?,
    /** Defined for "page_link" actions. Always a page within the current property. */
    @field:Json(name = "page_id")
    val pageId: String?,
    /** Defined for "link" actions. */
    val url: String?,

    /**
     * The button's text and styling. Actions also carry legacy "text"/"label"/"colors"/
     * "border_radius" fields, but the server only ever populates them with stale defaults - this
     * is the only definition anything reads. "button_style" is intentionally ignored.
     */
    val button: HeroActionButtonDto?,
)

@JsonClass(generateAdapter = true)
data class HeroActionButtonDto(
    val text: String?,
    @field:Json(name = "background_color")
    val backgroundColor: String?,
    @field:Json(name = "text_color")
    val textColor: String?,
    @field:Json(name = "border_color")
    val borderColor: String?,
    @field:Json(name = "border_radius")
    val borderRadius: Int?,
)
