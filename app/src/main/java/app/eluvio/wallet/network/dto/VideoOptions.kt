package app.eluvio.wallet.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoOptionsDto(
    @field:Json(name = "dash-clear") val dash_clear: PlayoutConfigDto?,
    @field:Json(name = "hls-clear") val hls_clear: PlayoutConfigDto?,
    @field:Json(name = "dash-widevine") val dash_widevine: PlayoutConfigDto?,
    @field:Json(name = "hls-widevine-cenc") val hls_widevine: PlayoutConfigDto?,
)

@JsonClass(generateAdapter = true)
data class PlayoutConfigDto(
    val properties: ProtocolPropertiesDto,
    val uri: String,
)

@JsonClass(generateAdapter = true)
data class ProtocolPropertiesDto(
    val protocol: String,
    val drm: String?,
    val start_offset_float: Float?,
    val start_offset_rat: String?,
    val license_servers: List<String>?,
)
