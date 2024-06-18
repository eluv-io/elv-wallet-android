package app.eluvio.wallet.network.dto.v2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaPageSectionDto(
    // List of IDs of the media items in this section
    val content: List<SectionItemDto>,
    val description: String?,
    val id: String,
    val type: String,
    val display: DisplaySettingsDto?,
)

@JsonClass(generateAdapter = true)
data class SectionItemDto(
    // Technically these have IDs, but we don't use them for anything.

    val type: String,
    @field:Json(name = "media_type")
    val mediaType: String?,
    val media: MediaItemV2Dto?,
    /**
     * Only applies to lists and collections.
     * If `true`, inline the list items in the section.
     */
    val expand: Boolean?,

    // TODO: handle this field
    @field:Json(name = "use_media_settings")
    val useMediaSettings: Boolean?
)