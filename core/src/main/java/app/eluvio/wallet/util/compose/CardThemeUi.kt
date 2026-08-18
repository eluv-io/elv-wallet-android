package app.eluvio.wallet.util.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.display.CardBorderRadius
import app.eluvio.wallet.data.entities.v2.display.CardEffect
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The card theme that applies to the cards currently being composed.
 * Provided per-Carousel, since every Section can define its own theme.
 */
val LocalCardTheme = compositionLocalOf<CardThemeEntity?> { null }

/**
 * The shape a card with the given [aspectRatio] should take.
 * [default] is used when there's no theme to apply.
 */
fun CardThemeEntity?.cardShape(aspectRatio: Float?, default: Shape): Shape {
    val theme = this ?: return default
    if (isCircular(aspectRatio)) {
        return CircleShape
    }
    return when (theme.borderRadius) {
        CardBorderRadius.NONE -> RectangleShape
        CardBorderRadius.SUBTLE -> RoundedCornerShape(5.dp)
        CardBorderRadius.CURVED -> RoundedCornerShape(20.dp)
    }
}

/**
 * Whether a card with the given [aspectRatio] renders as a circle under this theme.
 * Only square cards get circularized - any other aspect ratio would turn into an ellipse.
 * Circular cards need special treatment for anything drawn near their edges, since the corners
 * of the card are clipped away.
 */
fun CardThemeEntity?.isCircular(aspectRatio: Float?): Boolean =
    this != null && circularize && aspectRatio == AspectRatio.SQUARE

/**
 * Whether the theme draws a border of its own.
 * When it doesn't, cards keep whatever border the app draws by default (on TV, the focus ring).
 */
val CardThemeEntity?.hasBorder: Boolean get() = (this?.borderWidth ?: 0) > 0

/**
 * The border to draw around a card. Only meaningful when [hasBorder] is true.
 * [focused] selects between the theme's "active" and "inactive" states.
 */
fun CardThemeEntity.cardBorder(focused: Boolean): BorderStroke {
    val color = state(focused)?.borderColor ?: DEFAULT_BORDER_COLOR
    return BorderStroke(borderWidth.dp, Color.fromHex(color))
}

/**
 * How saturated a card's image should be: fully grey while the theme's desaturate effect
 * applies, full color otherwise. Like the web, focusing a card restores its color.
 */
fun CardThemeEntity?.imageSaturation(focused: Boolean): Float =
    if (this?.effect == CardEffect.DESATURATE && !focused) DESATURATED else FULLY_SATURATED

/** A filter that draws content at [saturation], or null when there's nothing to change. */
fun saturationFilter(saturation: Float): ColorFilter? {
    if (saturation >= FULLY_SATURATED) return null
    return ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
}

/**
 * The two colors a card's background is painted with, and the direction between them.
 * A solid background is just a gradient between two identical colors, which is how the web
 * renders it too.
 */
data class CardBackground(
    val startColor: Color,
    val endColor: Color,
    /** Degrees, clockwise from "up", like a CSS gradient. */
    val angleDegrees: Float,
)

/**
 * The background this theme paints behind a card's image, or null when there's no theme to apply.
 * A theme that doesn't name a color gets opaque black, matching the web's defaults.
 */
fun CardThemeEntity?.cardBackground(focused: Boolean): CardBackground? {
    val state = this?.state(focused) ?: return null
    val start = cardColor(state.backgroundColor, state.backgroundColorOpacity)
    val end = if (state.gradient) {
        cardColor(state.backgroundColor2, state.backgroundColor2Opacity)
    } else {
        start
    }
    return CardBackground(start, end, state.backgroundGradientAngle.toFloat())
}

/**
 * Paints the background as a two-stop linear gradient.
 *
 * [CardBackground.angleDegrees] is a CSS angle, so the gradient line is rotated clockwise from
 * "up" and sized to span the card in whatever direction it ends up pointing.
 */
fun CardBackground.toBrush(): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
        val length = abs(size.width * direction.x) + abs(size.height * direction.y)
        val center = Offset(size.width / 2f, size.height / 2f)
        val halfLine = direction * (length / 2f)
        return LinearGradientShader(
            from = center - halfLine,
            to = center + halfLine,
            colors = listOf(startColor, endColor),
        )
    }
}

private fun cardColor(hex: String?, opacityPercent: Int): Color =
    Color.fromHex(hex ?: DEFAULT_BACKGROUND_COLOR)
        .copy(alpha = opacityPercent.coerceIn(0, 100) / 100f)

private const val FULLY_SATURATED = 1f
private const val DESATURATED = 0f
private const val DEFAULT_BORDER_COLOR = "#FFFFFF"
private const val DEFAULT_BACKGROUND_COLOR = "#000000"
