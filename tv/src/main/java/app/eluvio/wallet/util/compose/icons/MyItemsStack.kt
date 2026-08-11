package app.eluvio.wallet.util.compose.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun VectorPreview() {
    Image(EluvioIcons.MyItemsStack, null)
}

private var _MyItemsStack: ImageVector? = null

/**
 * Matches tvOS's `rectangle.stack`: a rounded rectangle in front, with the top edges of two
 * progressively narrower cards peeking out above it.
 *
 * Distinct from [MyItems] (the portrait card the nav drawer tab uses) - this is the Property
 * page's action row, which mirrors tvOS icon for icon.
 */
public val EluvioIcons.MyItemsStack: ImageVector
    get() {
        if (_MyItemsStack != null) {
            return _MyItemsStack!!
        }
        _MyItemsStack = ImageVector.Builder(
            name = "MyItemsStack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val stroke = SolidColor(Color.Black)
            // Front card
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 9.5f)
                lineTo(19.5f, 9.5f)
                arcTo(2f, 2f, 0f, false, true, 21.5f, 11.5f)
                lineTo(21.5f, 18.5f)
                arcTo(2f, 2f, 0f, false, true, 19.5f, 20.5f)
                lineTo(4.5f, 20.5f)
                arcTo(2f, 2f, 0f, false, true, 2.5f, 18.5f)
                lineTo(2.5f, 11.5f)
                arcTo(2f, 2f, 0f, false, true, 4.5f, 9.5f)
                close()
            }
            // Card behind
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4.8f, 6.6f)
                lineTo(19.2f, 6.6f)
            }
            // Card behind that
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(7f, 3.7f)
                lineTo(17f, 3.7f)
            }
        }.build()
        return _MyItemsStack!!
    }
