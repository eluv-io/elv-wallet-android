package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import app.eluvio.wallet.R

open class MediaGlowTextButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val focusedPaint = Paint()
    private val unfocusedPaint = Paint()
    private var hasUnfocusedBackground = false

    private var cornerRadius: Float = 0f
    private var glowRadius: Float = 0f

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.MediaGlowTextButton,
            defStyleAttr, 0
        ) {
            glowRadius = getDimension(
                R.styleable.MediaGlowTextButton_glowRadius,
                5f * resources.displayMetrics.density
            )

            cornerRadius = getDimension(
                R.styleable.MediaGlowTextButton_cornerRadius,
                4f * resources.displayMetrics.density
            )

            val focusedBgColor =
                getColor(R.styleable.MediaGlowTextButton_focusedBackgroundColor, Color.WHITE)
            initFocusedPaint(focusedBgColor)

            val unfocusedBgColor =
                getColor(R.styleable.MediaGlowTextButton_unfocusedBackgroundColor, Color.TRANSPARENT)
            if (unfocusedBgColor != Color.TRANSPARENT) {
                hasUnfocusedBackground = true
                unfocusedPaint.color = unfocusedBgColor
            }
        }
    }

    private fun initFocusedPaint(focusedBgColor: Int) {
        // Not customizable
        val glowColor = Color.WHITE
        val dx = 0f
        val dy = 0f

        focusedPaint.color = focusedBgColor // This is the color of the shape itself
        focusedPaint.setShadowLayer(glowRadius, dx, dy, glowColor)
    }

    override fun onDraw(canvas: Canvas) {
        if (isFocused) {
            canvas.drawRoundRect(
                glowRadius,
                glowRadius,
                width.toFloat() - glowRadius,
                height.toFloat() - glowRadius,
                cornerRadius,
                cornerRadius,
                focusedPaint
            )
        } else if (hasUnfocusedBackground) {
            canvas.drawRoundRect(
                glowRadius,
                glowRadius,
                width.toFloat() - glowRadius,
                height.toFloat() - glowRadius,
                cornerRadius,
                cornerRadius,
                unfocusedPaint
            )
        }
        super.onDraw(canvas)
    }
}
