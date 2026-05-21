package app.eluvio.wallet.util.compose.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
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
    Image(EluvioIcons.MyItems, null)
}

private var _MyItems: ImageVector? = null

public val EluvioIcons.MyItems: ImageVector
    get() {
        if (_MyItems != null) {
            return _MyItems!!
        }
        _MyItems = ImageVector.Builder(
            name = "MyItems",
            defaultWidth = 39.dp,
            defaultHeight = 49.dp,
            viewportWidth = 39f,
            viewportHeight = 49f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7.3125f, 45.0635f)
                verticalLineTo(45.9302f)
                curveTo(7.3125f, 47.3662f, 8.4766f, 48.5303f, 9.9125f, 48.5303f)
                horizontalLineTo(36.4005f)
                curveTo(37.8365f, 48.5303f, 39.0005f, 47.3662f, 39.0005f, 45.9302f)
                verticalLineTo(9.52972f)
                curveTo(39.0005f, 8.0938f, 37.8365f, 6.9297f, 36.4005f, 6.9297f)
                horizontalLineTo(35.7505f)
                verticalLineTo(42.4635f)
                curveTo(35.7505f, 43.8995f, 34.5864f, 45.0635f, 33.1504f, 45.0635f)
                horizontalLineTo(7.3125f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(0f, 2.60004f)
                curveTo(0f, 1.1641f, 1.1641f, 0f, 2.6f, 0f)
                horizontalLineTo(29.4671f)
                curveTo(30.903f, 0f, 32.0671f, 1.1641f, 32.0671f, 2.6f)
                verticalLineTo(39.0005f)
                curveTo(32.0671f, 40.4365f, 30.903f, 41.6006f, 29.4671f, 41.6006f)
                horizontalLineTo(2.60003f)
                curveTo(1.1641f, 41.6006f, 0f, 40.4365f, 0f, 39.0005f)
                verticalLineTo(2.60004f)
                close()
                moveTo(3.46671f, 5.20007f)
                horizontalLineTo(28.6004f)
                verticalLineTo(31.2004f)
                horizontalLineTo(3.46671f)
                verticalLineTo(5.20007f)
                close()
                moveTo(28.6004f, 33.8005f)
                horizontalLineTo(3.46671f)
                verticalLineTo(36.4005f)
                horizontalLineTo(28.6004f)
                verticalLineTo(33.8005f)
                close()
            }
        }.build()
        return _MyItems!!
    }
