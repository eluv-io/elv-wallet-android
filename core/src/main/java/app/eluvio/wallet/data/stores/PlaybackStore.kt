package app.eluvio.wallet.data.stores

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.max

/**
 * Keeps track of the playback position for each media item.
 */
class PlaybackStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "playback_store", Context.MODE_PRIVATE
    )

    /**
     * Get the playback position for a media item.
     * Returns 0 if no position is stored.
     */
    fun getPlaybackPosition(mediaId: String): Long {
        val position = prefs.getLong("$mediaId$POSITION_SUFFIX", -1)
            .takeIf { it != -1L }
        // Before we started saving the progress, we saved the position under [mediaId] as
        // key with no suffix, so use that as a fallback
            ?: prefs.getLong(mediaId, 0)
        // Safeguard against bad (negative) values
        return max(0, position)
    }

    /**
     * Get the watched progress for a media item as a fraction of the total duration (0-1).
     */
    fun getPlaybackProgress(mediaId: String): Float {
        return prefs.getFloat("$mediaId$PROGRESS_SUFFIX", 0f).coerceIn(0f, 1f)
    }

    /**
     * Set the playback position for a media item.
     */
    fun setPlaybackPosition(mediaId: String, position: Long, totalDuration: Long) {
        prefs.edit {
            putLong("$mediaId$POSITION_SUFFIX", position)
            putFloat("$mediaId$PROGRESS_SUFFIX", position.toFloat() / totalDuration)
        }
    }

    fun wipe() {
        prefs.edit().clear().apply()
    }
}

private const val POSITION_SUFFIX = "_position"
private const val PROGRESS_SUFFIX = "_progress"
