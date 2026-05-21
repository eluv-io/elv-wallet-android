package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Conditionally applies a dimming effect to the content of this modifier.
 */
fun Modifier.dimContent(dim: Boolean = true, color: Color = Color.Black.copy(alpha = 0.8f)) =
    drawWithCache {
        onDrawWithContent {
            drawContent()
            if (dim) {
                drawRect(color)
            }
        }
    }

/**
 * Like [Modifier.offset], but also fakes the size of the layout, so other items
 * will not position themselves as if the offset doesn't exist.
 */
fun Modifier.offsetAndFakeSize(xOffset: Dp = 0.dp, yOffset: Dp = 0.dp) =
    layout { measurable, constraints ->
        val xPx = xOffset.roundToPx()
        val yPx = yOffset.roundToPx()
        val placeable = measurable.measure(constraints)
        layout(placeable.width + xPx, placeable.height + yPx) {
            placeable.placeRelative(xPx, yPx)
        }
    }
