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
    Image(EluvioIcons.Search, null)
}

private var _Search: ImageVector? = null

public val EluvioIcons.Search: ImageVector
    get() {
        if (_Search != null) {
            return _Search!!
        }
        _Search = ImageVector.Builder(
            name = "Search",
            defaultWidth = 30.dp,
            defaultHeight = 32.dp,
            viewportWidth = 30f,
            viewportHeight = 32f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFC7C7C8)),
                fillAlpha = 0.8f,
                stroke = null,
                strokeAlpha = 0.8f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(13.3441f, 3.12088e-8f)
                curveTo(11.216f, 0.0002f, 9.1189f, 0.5281f, 7.2277f, 1.5397f)
                curveTo(5.3364f, 2.5513f, 3.7058f, 4.0172f, 2.472f, 5.8152f)
                curveTo(1.2382f, 7.6132f, 0.4369f, 9.6911f, 0.135f, 11.8755f)
                curveTo(-0.1669f, 14.0599f, 0.0393f, 16.2876f, 0.7364f, 18.3726f)
                curveTo(1.4336f, 20.4575f, 2.6014f, 22.3394f, 4.1425f, 23.8612f)
                curveTo(5.6837f, 25.3829f, 7.5534f, 26.5004f, 9.5957f, 27.1204f)
                curveTo(11.638f, 27.7405f, 13.7936f, 27.845f, 15.8828f, 27.4254f)
                curveTo(17.972f, 27.0058f, 19.9342f, 26.0741f, 21.6055f, 24.7082f)
                lineTo(27.3393f, 30.6541f)
                curveTo(27.6354f, 30.9507f, 28.032f, 31.1148f, 28.4436f, 31.111f)
                curveTo(28.8553f, 31.1073f, 29.2491f, 30.9361f, 29.5402f, 30.6342f)
                curveTo(29.8312f, 30.3324f, 29.9964f, 29.924f, 29.9999f, 29.4971f)
                curveTo(30.0035f, 29.0703f, 29.8453f, 28.659f, 29.5593f, 28.3519f)
                lineTo(23.8256f, 22.4061f)
                curveTo(25.3767f, 20.3654f, 26.3426f, 17.9131f, 26.6125f, 15.3299f)
                curveTo(26.8824f, 12.7466f, 26.4456f, 10.1367f, 25.352f, 7.7988f)
                curveTo(24.2583f, 5.461f, 22.5521f, 3.4896f, 20.4285f, 2.1104f)
                curveTo(18.3049f, 0.7312f, 15.8498f, -0.0002f, 13.3441f, 0f)
                close()
                moveTo(3.13891f, 13.8389f)
                curveTo(3.1389f, 11.0322f, 4.2141f, 8.3405f, 6.1279f, 6.3558f)
                curveTo(8.0418f, 4.3712f, 10.6375f, 3.2562f, 13.3441f, 3.2562f)
                curveTo(16.0506f, 3.2562f, 18.6464f, 4.3712f, 20.5602f, 6.3558f)
                curveTo(22.474f, 8.3405f, 23.5492f, 11.0322f, 23.5492f, 13.8389f)
                curveTo(23.5492f, 16.6457f, 22.474f, 19.3374f, 20.5602f, 21.3221f)
                curveTo(18.6464f, 23.3067f, 16.0506f, 24.4217f, 13.3441f, 24.4217f)
                curveTo(10.6375f, 24.4217f, 8.0418f, 23.3067f, 6.1279f, 21.3221f)
                curveTo(4.2141f, 19.3374f, 3.1389f, 16.6457f, 3.1389f, 13.8389f)
                close()
            }
        }.build()
        return _Search!!
    }
