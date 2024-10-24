package app.eluvio.wallet.util.compose.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.util.compose.icons.EluvioIcons
import app.eluvio.wallet.util.compose.icons.MyItems

@Preview
@Composable
private fun VectorPreview() {
	Image(EluvioIcons.Switcher, null)
}

private var _Switcher: ImageVector? = null

public val EluvioIcons.Switcher: ImageVector
	get() {
		if (_Switcher != null) {
			return _Switcher!!
		}
		_Switcher = ImageVector.Builder(
            name = "Switcher",
            defaultWidth = 33.dp,
            defaultHeight = 31.dp,
            viewportWidth = 33f,
            viewportHeight = 31f
        ).apply {
			group {
				path(
    				fill = SolidColor(Color(0xFF2D2D2D)),
    				fillAlpha = 1.0f,
    				stroke = null,
    				strokeAlpha = 1.0f,
    				strokeLineWidth = 1.0f,
    				strokeLineCap = StrokeCap.Butt,
    				strokeLineJoin = StrokeJoin.Miter,
    				strokeLineMiter = 1.0f,
    				pathFillType = PathFillType.NonZero
				) {
					moveTo(2.52001f, 9.2744f)
					horizontalLineTo(25.4f)
					lineTo(22.584f, 11.375f)
					curveTo(22.39910f, 11.51290f, 22.24330f, 11.68570f, 22.12560f, 11.88350f)
					curveTo(22.00780f, 12.08130f, 21.93040f, 12.30020f, 21.89770f, 12.52780f)
					curveTo(21.8650f, 12.75530f, 21.87770f, 12.98710f, 21.93510f, 13.20980f)
					curveTo(21.99240f, 13.43250f, 22.09330f, 13.64170f, 22.2320f, 13.82560f)
					curveTo(22.39590f, 14.0430f, 22.60850f, 14.21950f, 22.85290f, 14.3410f)
					curveTo(23.09730f, 14.46250f, 23.36680f, 14.52580f, 23.640f, 14.52580f)
					curveTo(24.02080f, 14.52580f, 24.39140f, 14.4030f, 24.6960f, 14.17570f)
					lineTo(31.736f, 8.92431f)
					curveTo(31.95130f, 8.76090f, 32.12570f, 8.55040f, 32.24580f, 8.3090f)
					curveTo(32.36590f, 8.06770f, 32.42840f, 7.8020f, 32.42840f, 7.53270f)
					curveTo(32.42840f, 7.26340f, 32.36590f, 6.99770f, 32.24580f, 6.75640f)
					curveTo(32.12570f, 6.5150f, 31.95130f, 6.30440f, 31.7360f, 6.14110f)
					lineTo(24.9424f, 0.889658f)
					curveTo(24.57370f, 0.60410f, 24.1060f, 0.4760f, 23.64220f, 0.53350f)
					curveTo(23.17850f, 0.59090f, 22.75670f, 0.82920f, 22.46960f, 1.1960f)
					curveTo(22.18250f, 1.56280f, 22.05370f, 2.02790f, 22.11150f, 2.48910f)
					curveTo(22.16920f, 2.95040f, 22.40890f, 3.36990f, 22.77760f, 3.65540f)
					lineTo(25.5408f, 5.77346f)
					horizontalLineTo(2.52001f)
					curveTo(2.05320f, 5.77350f, 1.60560f, 5.95790f, 1.27550f, 6.28620f)
					curveTo(0.94540f, 6.61440f, 0.760f, 7.05970f, 0.760f, 7.52390f)
					curveTo(0.760f, 7.98820f, 0.94540f, 8.43340f, 1.27550f, 8.76170f)
					curveTo(1.60560f, 9.090f, 2.05320f, 9.27440f, 2.520f, 9.27440f)
					close()
					moveTo(30.68f, 21.5277f)
					horizontalLineTo(7.80001f)
					lineTo(10.616f, 19.4271f)
					curveTo(10.98940f, 19.14860f, 11.23630f, 18.73390f, 11.30230f, 18.27430f)
					curveTo(11.36830f, 17.81470f, 11.24810f, 17.34790f, 10.9680f, 16.97650f)
					curveTo(10.68790f, 16.60510f, 10.2710f, 16.35950f, 9.80890f, 16.29390f)
					curveTo(9.34680f, 16.22820f, 8.87740f, 16.34780f, 8.5040f, 16.62640f)
					lineTo(1.46401f, 21.8778f)
					curveTo(1.24880f, 22.04110f, 1.07430f, 22.25170f, 0.95420f, 22.49310f)
					curveTo(0.83410f, 22.73440f, 0.77160f, 23.00010f, 0.77160f, 23.26940f)
					curveTo(0.77160f, 23.53870f, 0.83410f, 23.80440f, 0.95420f, 24.04570f)
					curveTo(1.07430f, 24.28710f, 1.24880f, 24.49770f, 1.4640f, 24.6610f)
					lineTo(8.25761f, 29.9124f)
					curveTo(8.56480f, 30.14960f, 8.94230f, 30.27880f, 9.33120f, 30.280f)
					curveTo(9.60f, 30.27940f, 9.86510f, 30.21750f, 10.10610f, 30.09920f)
					curveTo(10.34710f, 29.98080f, 10.55770f, 29.80920f, 10.72160f, 29.59730f)
					curveTo(11.00740f, 29.23240f, 11.13640f, 28.76980f, 11.08030f, 28.31060f)
					curveTo(11.02430f, 27.85140f, 10.78770f, 27.4330f, 10.42240f, 27.14670f)
					lineTo(7.65921f, 25.0286f)
					horizontalLineTo(30.68f)
					curveTo(31.14680f, 25.02860f, 31.59450f, 24.84420f, 31.92450f, 24.51590f)
					curveTo(32.25460f, 24.18760f, 32.440f, 23.74240f, 32.440f, 23.27810f)
					curveTo(32.440f, 22.81390f, 32.25460f, 22.36870f, 31.92450f, 22.04040f)
					curveTo(31.59450f, 21.71210f, 31.14680f, 21.52770f, 30.680f, 21.52770f)
					close()
				}
}
		}.build()
		return _Switcher!!
	}
