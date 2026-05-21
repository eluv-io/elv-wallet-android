package app.eluvio.wallet.data

object AspectRatio {
    const val SQUARE = 1f
    const val WIDE = 16f / 9f // A.K.A. "Landscape"
    const val POSTER = 2f / 3f // A.K.A. "Portrait"

    fun parse(aspectRatioString: String?): Float? {
        return when (aspectRatioString) {
            "Square" -> SQUARE
            "Wide", "Landscape" -> WIDE
            "Poster", "Portrait" -> POSTER
            else -> null
        }
    }
}
