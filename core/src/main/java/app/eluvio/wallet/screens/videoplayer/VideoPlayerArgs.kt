package app.eluvio.wallet.screens.videoplayer

import kotlinx.serialization.Serializable

/** Intent extra key for the serialized [VideoPlayerArgs] JSON. */
const val VIDEO_PLAYER_ARGS_EXTRA = "video_player_args_json"

@Serializable
data class VideoPlayerArgs(
    val mediaItemId: String,
    // Given as a convenience to avoid delaying video load just for analytics purposes
    val mediaTitle: String? = null,
    val propertyId: String? = null,
    /** if this is supplied, just play the first featured video */
    val deeplinkhack_contract: String? = null,
)
