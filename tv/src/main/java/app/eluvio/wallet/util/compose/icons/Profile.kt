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
    Image(EluvioIcons.Profile, null)
}

private var _Profile: ImageVector? = null

/**
 * Matches tvOS's `person.crop.circle`: an outlined ring with a *filled* person inside.
 *
 * Material's AccountCircle doesn't offer that combination - the filled variant is a solid disc
 * with the person knocked out, and the outlined variant hollows out the person too.
 */
public val EluvioIcons.Profile: ImageVector
    get() {
        if (_Profile != null) {
            return _Profile!!
        }
        _Profile = ImageVector.Builder(
            name = "Profile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Ring. Sized to fill the viewport: a thin outline circle reads optically smaller
            // than the solid rect/magnifier glyphs it sits next to, so it needs the extra size
            // to look like a peer. Matches tvOS, where the glyph fills ~57% of the button.
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 1f,
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(1f, 12f)
                arcTo(11f, 11f, 0f, true, true, 23f, 12f)
                arcTo(11f, 11f, 0f, true, true, 1f, 12f)
                close()
            }
            // Head
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.456f, 9.8f)
                arcTo(3.544f, 3.544f, 0f, true, true, 15.544f, 9.8f)
                arcTo(3.544f, 3.544f, 0f, true, true, 8.456f, 9.8f)
                close()
            }
            // Shoulders: the part of the body circle that falls inside the ring. The arc
            // endpoints are where the two circles intersect, so they land exactly on the ring
            // instead of needing a clip.
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.557f, 20.1f)
                arcTo(7.578f, 7.578f, 0f, false, true, 19.443f, 20.1f)
                arcTo(11f, 11f, 0f, false, true, 4.557f, 20.1f)
                close()
            }
        }.build()
        return _Profile!!
    }
