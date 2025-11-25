package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap
import app.eluvio.wallet.R

class MediaGlowIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val unfocusedPaint = Paint()
    private val focusedPaint = Paint()

    // Used to capture pixels drawn by "src" and avoid drawing them in the background layer.
    private val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private var glowRadius: Float = 0f

    private var iconBitmap: Bitmap? = null
    private var iconCanvas: Canvas? = null

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.MediaGlowIconButton,
            defStyleAttr, 0
        ) {
            glowRadius = getDimension(
                R.styleable.MediaGlowIconButton_glowRadius,
                5f * resources.displayMetrics.density
            )

            val unfocusedBgColor =
                getColor(R.styleable.MediaGlowIconButton_unfocusedBackgroundColor, Color.WHITE)
            val focusedBgColor =
                getColor(R.styleable.MediaGlowIconButton_focusedBackgroundColor, Color.WHITE)
            initPaints(unfocusedBgColor, focusedBgColor)
        }

        // Start assuming the button is not focused.
        alpha = 0.6f
    }

    private fun initPaints(unfocusedBgColor: Int, focusedBgColor: Int) {
        unfocusedPaint.color = unfocusedBgColor

        val dx = 0f
        val dy = 0f
        val shadowColor = Color.WHITE

        focusedPaint.color = focusedBgColor // This is the color of the shape itself
        focusedPaint.setShadowLayer(glowRadius, dx, dy, shadowColor)
    }

    override fun setAlpha(alpha: Float) {
        // Exoplayer keeps trying to mess with this value, so we trick it into sending 2.0f
        // and then we know to ignore it
        if (alpha > 1f) return

        super.setAlpha(alpha)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        animateAlpha(gainFocus)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        animateAlpha()
    }

    private fun animateAlpha(isFocused: Boolean = this.isFocused) {
        animate()
            .setInterpolator(AccelerateInterpolator())
            .setDuration(100)
            .alpha(if (isFocused) 1f else 0.6f)
            .start()
    }

    override fun onDraw(canvas: Canvas) {
        val paint = if (isFocused) {
            focusedPaint
        } else {
            unfocusedPaint
        }

        // Create an offscreen bitmap for the icon if we need to.
        // This is where we'll draw the icon to be used as a "punch"
        if (iconBitmap == null || iconBitmap?.width != width || iconBitmap?.height != height) {
            iconBitmap?.recycle()
            iconBitmap = createBitmap(width, height)
            iconCanvas = Canvas(iconBitmap!!)
        }
        val localIconBitmap = iconBitmap ?: return
        val localIconCanvas = iconCanvas ?: return

        // Draw the icon onto our offscreen bitmap.
        localIconBitmap.eraseColor(Color.TRANSPARENT)
        super.onDraw(localIconCanvas)

        // Save the canvas layer to apply the xfermode.
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // Draw the circle background (the destination).
        canvas.drawCircle(
            width / 2f,
            height / 2f,
            width / 2f - glowRadius,
            paint
        )

        // Draw the icon bitmap with the xfermode to punch the hole.
        canvas.drawBitmap(localIconBitmap, 0f, 0f, xferPaint)

        // Restore the canvas layer.
        canvas.restoreToCount(save)

        // Draw the icon again, normally, on top of the circle with the hole.
        super.onDraw(canvas)
    }
}
