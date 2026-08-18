package app.eluvio.wallet.screens.common

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The focused-card ring from the design: a thin white stroke whose bright segment sweeps around
 * the card, one revolution per 3.6s.
 *
 * Compose's Brush.sweepGradient can't rotate its start angle, so the ring is drawn with a
 * framework [SweepGradient] whose local matrix is rotated each frame. The angle is only read at
 * draw time, so the animation invalidates the draw phase without recomposing.
 */
@Composable
fun AnimatedFocusRing(
    shape: Shape,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.dp,
) {
    val angle by rememberInfiniteTransition(label = "focusRing")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
            label = "ringAngle"
        )
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val strokePx = strokeWidth.toPx()
                val inset = strokePx / 2
                // The stroke straddles the path, so run it half a stroke inside the card's
                // outline - otherwise the outer half gets clipped away.
                val outline = shape.createOutline(
                    Size(size.width - strokePx, size.height - strokePx),
                    layoutDirection,
                    this
                )
                val path = Path()
                    .apply { addOutline(outline) }
                    .apply { translate(Offset(inset, inset)) }
                    .asAndroidPath()
                val matrix = Matrix()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokePx
                    shader = SweepGradient(
                        size.width / 2,
                        size.height / 2,
                        intArrayOf(RingBaseColor, RingPeakColor, RingBaseColor, RingBaseColor),
                        // Bright segment peaks at 90° and fades back out by 200°.
                        floatArrayOf(0f, 90 / 360f, 200 / 360f, 1f)
                    )
                }
                onDrawBehind {
                    // The design's conic gradient starts at 12 o'clock; SweepGradient starts
                    // at 3 o'clock, so shift by -90°.
                    matrix.setRotate(angle - 90f, size.width / 2, size.height / 2)
                    paint.shader.setLocalMatrix(matrix)
                    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
                }
            }
    )
}

// Framework colors for the focus ring's SweepGradient shader.
private val RingBaseColor = android.graphics.Color.argb(41, 255, 255, 255) // white @ 16%
private val RingPeakColor = android.graphics.Color.WHITE
