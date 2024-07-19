package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.MediaLinkDto
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
    val type: String,
    @field:Json(name = "poster_image")
    val posterImage: AssetLinkDto?,
    @field:Json(name = "thumbnail_image_landscape")
    val thumbnailLandscape: AssetLinkDto?,
    @field:Json(name = "thumbnail_image_portrait")
    val thumbnailPortrait: AssetLinkDto?,
    @field:Json(name = "thumbnail_image_square")
    val thumbnailSquare: AssetLinkDto?,
    val title: String?,
    // Media of type "list" will have a list of media item ids
    val media: List<String>?,
    // Media of type "collection" will have a list of media list ids
    @field:Json(name = "media_lists")
    val mediaLists: List<String>?,
    val gallery: List<GalleryItemV2Dto>?,

    // Live Video info
    @field:Json(name = "live_video")
    val liveVideo: Boolean?,
    @field:Json(name = "start_time")
    val startTime: Date?,
    @field:Json(name = "end_time")
    val endTime: Date?,
    val subtitle: String?,
    val headers: List<String>?,
    val icons: List<MediaIconDto>?,

    // Search related stuff
    val attributes: Map<String, List<String>>?,
    val tags: List<String>?,
)

@JsonClass(generateAdapter = true)
data class MediaIconDto(
    val icon: AssetLinkDto?,
)

@JsonClass(generateAdapter = true)
data class GalleryItemV2Dto(
    val thumbnail: AssetLinkDto,
    @field:Json(name = "thumbnail_aspect_ratio")
    val thumbnailAspectRatio: String?,

    // TODO: image should take precedence over thumbnail, if it exists
    //    val image: AssetLinkDto?,
    //    @field:Json(name = "image_aspect_ratio")
    //    val imageAspectRatio: String?,

    //    val video: ...?
)
