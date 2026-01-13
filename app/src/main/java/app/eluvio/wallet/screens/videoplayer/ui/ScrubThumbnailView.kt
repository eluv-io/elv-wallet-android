package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.media.ThumbnailSprite

/**
 * A view that displays a thumbnail preview during video scrubbing.
 * Position this view above the seek bar and call [updatePosition] during scrub events.
 */
class ScrubThumbnailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var thumbnailSprite: ThumbnailSprite? = null
    private var sourceRect: Rect? = null
    private var scrubFraction: Float = 0f

    private val thumbnailPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dp
        color = 0xFFFFFFFF.toInt()
    }

    private val destRect = RectF()
    private val borderRadius = 4.dp

    // Thumbnail dimensions:
    // width is configurable with default.
    var thumbnailWidth = 160.dp

    // height is calculated based on the first cue aspect ratio and given width
    var thumbnailHeight = 0f
        private set

    init {
        // Start invisible until we have a thumbnail to show
        visibility = GONE
    }

    /**
     * Sets the thumbnail sprite sheet data.
     * Call this after loading thumbnails from the WebVTT.
     */
    fun setThumbnailSprite(sprite: ThumbnailSprite?) {
        thumbnailSprite = sprite
        if (sprite != null && sprite.cues.isNotEmpty()) {
            // Set thumbnail dimensions based on the first cue
            val firstCue = sprite.cues.first()
            val aspectRatio = firstCue.rect.width().toFloat() / firstCue.rect.height()
            thumbnailHeight = thumbnailWidth / aspectRatio
            Log.d("Thumbnail dimensions: ${thumbnailWidth}x${thumbnailHeight}")
        }
        invalidate()
    }

    /**
     * Updates the scrub position and refreshes the thumbnail.
     *
     * @param positionMs The current scrub position in milliseconds
     * @param fraction The position as a fraction (0-1) of the seek bar width,
     *                 used to position the thumbnail horizontally
     */
    fun updatePosition(positionMs: Long, fraction: Float) {
        scrubFraction = fraction

        val sprite = thumbnailSprite
        if (sprite?.bitmap == null) {
            visibility = GONE
            return
        }

        sourceRect = sprite.getSourceRect(positionMs)
        visibility = if (sourceRect != null) VISIBLE else GONE
        invalidate()
    }

    /**
     * Hides the thumbnail preview.
     */
    fun hide() {
        visibility = GONE
        sourceRect = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val sprite = thumbnailSprite ?: return
        val bitmap = sprite.bitmap ?: return
        val srcRect = sourceRect ?: return

        // Calculate horizontal position based on scrub fraction
        // Clamp so thumbnail doesn't go off screen
        val centerX = width * scrubFraction
        val halfWidth = thumbnailWidth / 2
        val padding = borderPaint.strokeWidth
        val left = (centerX - halfWidth + padding).coerceIn(padding, width - thumbnailWidth - padding)

        // Position thumbnail above the bottom of this view (where the seek bar would be)
        destRect.set(
            left,
            height - thumbnailHeight - padding,
            left + thumbnailWidth,
            height - padding
        )

        // Draw thumbnail directly from sprite sheet - no intermediate bitmap allocation
        canvas.drawBitmap(bitmap, srcRect, destRect, thumbnailPaint)

        // Draw border
        canvas.drawRoundRect(destRect, borderRadius, borderRadius, borderPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Height should accommodate the thumbnail plus some padding
        val desiredHeight = thumbnailHeight + (borderPaint.strokeWidth * 2)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, desiredHeight.toInt())
    }

    private val Int.dp: Float get() = this * resources.displayMetrics.density
}
