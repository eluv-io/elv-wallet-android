package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.MediaLinkDto
import app.eluvio.wallet.network.dto.PlayableHashDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class MediaItemV2Dto(
    val id: String,
    @field:Json(name = "media_file")
    val mediaFile: AssetLinkDto?,
    @field:Json(name = "media_link")
    val mediaLink: MediaLinkDto?,
    @field:Json(name = "media_type")
    val mediaType: String?,
    @field:Json(name = "image_hash")
    val imageHash: String?,
    val type: String,
    @field:Json(name = "poster_image")
    val posterImage: AssetLinkDto?,
    // Media of type "list" will have a list of media item ids
    val media: List<String>?,
    // Media of type "collection" will have a list of media list ids
    @field:Json(name = "media_lists")
    val mediaLists: List<String>?,
    val gallery: List<GalleryItemV2Dto>?,
    @field:Json(name = "additional_views")
    val additionalViews: List<AdditionalViewDto>?,

    // Live Video info
    @field:Json(name = "live_video")
    val liveVideo: Boolean?,
    @field:Json(name = "start_time")
    val eventStartTime: Date?,
    @field:Json(name = "stream_start_time")
    val streamStartTime: Date?,
    @field:Json(name = "end_time")
    val endTime: Date?,
    val icons: List<MediaIconDto>?,

    // Search related stuff
    val attributes: Map<String, List<String>>?,
    val tags: List<String>?,
    val permissions: List<MediaPermissionsDto>?,
    // Media that is set to public=false, but also doesn't define any Permissions - is inaccessible.
    val public: Boolean?,

    override val title: String?,
    override val subtitle: String?,
    override val headers: List<String>?,
    override val description: String?,
    override val aspect_ratio: String?,
    override val thumbnail_image_landscape: AssetLinkDto?,
    override val thumbnail_image_portrait: AssetLinkDto?,
    override val thumbnail_image_square: AssetLinkDto?,
    override val display_limit: Int?,
    override val display_limit_type: String?,
    override val display_format: String?,
    override val logo: AssetLinkDto?,
    override val logo_text: String?,
    override val inline_background_color: String?,
    override val inline_background_image: AssetLinkDto?,
    override val background_image: AssetLinkDto?,
    override val background_video: PlayableHashDto?,
    override val hide_on_tv: Boolean?,
    override val full_bleed: Boolean?
) : DisplaySettingsDto

@JsonClass(generateAdapter = true)
data class MediaPermissionsDto(
    val permission_item_id: String?,
)

@JsonClass(generateAdapter = true)
data class MediaIconDto(
    val icon: AssetLinkDto?,
)

@JsonClass(generateAdapter = true)
data class AdditionalViewDto(
    val title: String?,
    // Legacy field. In the future, [title] will be the correct field.
    val label: String?,
    val image_tv: AssetLinkDto?,
    val image: AssetLinkDto?,
    @field:Json(name = "image_hash")
    val imageHash: String?,
    @field:Json(name = "media_link")
    val mediaLink: MediaLinkDto?,
)

@JsonClass(generateAdapter = true)
data class GalleryItemV2Dto(
    val thumbnail: AssetLinkDto?,
    @field:Json(name = "thumbnail_aspect_ratio")
    val thumbnailAspectRatio: String?,

    // TODO: image should take precedence over thumbnail, if it exists
    //    val image: AssetLinkDto?,
    //    @field:Json(name = "image_aspect_ratio")
    //    val imageAspectRatio: String?,

    //    val video: ...?
)
