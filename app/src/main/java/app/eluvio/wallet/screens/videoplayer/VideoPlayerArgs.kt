package app.eluvio.wallet.screens.videoplayer

data class VideoPlayerArgs(
    val mediaItemId: String,
    // Given as a convenience to avoid delaying video load just for analytics purposes
    val mediaTitle: String? = null,
    val propertyId: String? = null,
    /** if this is supplied, just play the first featured video */
    val deeplinkhack_contract: String? = null,
)
