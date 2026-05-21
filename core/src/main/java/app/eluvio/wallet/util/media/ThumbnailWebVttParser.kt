package app.eluvio.wallet.util.media

import android.graphics.Rect
import app.eluvio.wallet.util.logging.Log

/**
 * Parser for WebVTT files that describe sprite sheet thumbnails.
 *
 * Expected format:
 * ```
 * WEBVTT
 *
 * 00:00:00.000 --> 00:00:05.000
 * sprite.jpg#xywh=0,0,160,90
 *
 * 00:00:05.000 --> 00:00:10.000
 * sprite.jpg#xywh=160,0,160,90
 * ```
 */
object ThumbnailWebVttParser {

    private val TIMESTAMP_PATTERN = Regex(
        """(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})"""
    )

    private val XYWH_PATTERN = Regex(
        """#xywh=(\d+),(\d+),(\d+),(\d+)"""
    )

    /**
     * Parses a WebVTT string and returns a list of thumbnail cues.
     *
     * @param vttContent The content of the WebVTT file
     * @param baseUrl The base URL to resolve relative image URLs against
     * @return List of parsed thumbnail cues, or empty list if parsing fails
     */
    fun parse(vttContent: String, baseUrl: String): List<ThumbnailCue> {
        val cues = mutableListOf<ThumbnailCue>()
        val lines = vttContent.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()

            // Look for timestamp line
            val timestampMatch = TIMESTAMP_PATTERN.find(line)
            if (timestampMatch != null) {
                val startTimeMs = parseTimestamp(timestampMatch, isStart = true)
                val endTimeMs = parseTimestamp(timestampMatch, isStart = false)

                // Next non-empty line should be the image URL
                i++
                while (i < lines.size && lines[i].isBlank()) i++

                if (i < lines.size) {
                    val imageLine = lines[i].trim()
                    val cue = parseImageLine(imageLine, startTimeMs, endTimeMs, baseUrl)
                    if (cue != null) {
                        cues.add(cue)
                    }
                }
            }
            i++
        }

        Log.d("Parsed ${cues.size} thumbnail cues from WebVTT")
        return cues
    }

    private fun parseTimestamp(match: MatchResult, isStart: Boolean): Long {
        val offset = if (isStart) 0 else 4
        val hours = match.groupValues[1 + offset].toLong()
        val minutes = match.groupValues[2 + offset].toLong()
        val seconds = match.groupValues[3 + offset].toLong()
        val millis = match.groupValues[4 + offset].toLong()

        return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
    }

    private fun parseImageLine(
        line: String,
        startTimeMs: Long,
        endTimeMs: Long,
        baseUrl: String
    ): ThumbnailCue? {
        val xywhMatch = XYWH_PATTERN.find(line)
        if (xywhMatch == null) {
            Log.w("No xywh fragment found in thumbnail line: $line")
            return null
        }

        val x = xywhMatch.groupValues[1].toInt()
        val y = xywhMatch.groupValues[2].toInt()
        val width = xywhMatch.groupValues[3].toInt()
        val height = xywhMatch.groupValues[4].toInt()

        // Extract the image URL (everything before the #xywh fragment)
        val imageUrlPart = line.substringBefore("#xywh=")
        val imageUrl = resolveUrl(imageUrlPart, baseUrl)

        return ThumbnailCue(
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            imageUrl = imageUrl,
            rect = Rect(x, y, x + width, y + height)
        )
    }

    private fun resolveUrl(url: String, baseUrl: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> {
                // Absolute path - extract host from baseUrl
                val hostEnd = baseUrl.indexOf("/", baseUrl.indexOf("://") + 3)
                if (hostEnd > 0) {
                    baseUrl.take(hostEnd) + url
                } else {
                    baseUrl + url
                }
            }

            else -> {
                // Relative path
                "${baseUrl.removeSuffix("/")}/$url"
            }
        }
    }
}
