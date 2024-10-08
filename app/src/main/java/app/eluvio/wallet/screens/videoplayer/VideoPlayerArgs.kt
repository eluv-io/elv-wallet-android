package app.eluvio.wallet.screens.videoplayer

data class VideoPlayerArgs(
    val mediaItemId: String,
    val propertyId: String? = null,
    /** if this is supplied, just play the first featured video */
    val deeplinkhack_contract: String? = null,
)
