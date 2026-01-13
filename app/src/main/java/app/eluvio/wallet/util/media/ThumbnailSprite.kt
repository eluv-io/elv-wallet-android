package app.eluvio.wallet.util.media

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Represents a single thumbnail entry from a WebVTT sprite sheet.
 */
data class ThumbnailCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val imageUrl: String,
    val rect: Rect
)

/**
 * A parsed sprite sheet with all thumbnail cues and the associated bitmap.
 */
data class ThumbnailSprite(
    val cues: List<ThumbnailCue>,
    val bitmap: Bitmap?
) {
    /**
     * Finds the thumbnail cue for a given position in milliseconds.
     * Uses binary search for efficiency.
     */
    fun getCueForPosition(positionMs: Long): ThumbnailCue? {
        if (cues.isEmpty()) return null

        var low = 0
        var high = cues.lastIndex

        while (low <= high) {
            val mid = (low + high) / 2
            val cue = cues[mid]

            when {
                positionMs < cue.startTimeMs -> high = mid - 1
                positionMs >= cue.endTimeMs -> low = mid + 1
                else -> return cue
            }
        }

        // Return the closest cue if position is out of range
        return when {
            positionMs < cues.first().startTimeMs -> cues.first()
            positionMs >= cues.last().endTimeMs -> cues.last()
            else -> null
        }
    }

    /**
     * Returns the source rect within the sprite sheet for the given position.
     * Use this with [bitmap] to draw directly without creating intermediate bitmaps.
     */
    fun getSourceRect(positionMs: Long): Rect? {
        return getCueForPosition(positionMs)?.rect
    }
}
