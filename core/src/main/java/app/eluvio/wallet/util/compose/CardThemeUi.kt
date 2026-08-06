package app.eluvio.wallet.util.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.display.CardBorderRadius
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity

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
    val color = (if (focused) activeBorderColor else inactiveBorderColor) ?: DEFAULT_BORDER_COLOR
    return BorderStroke(borderWidth.dp, Color.fromHex(color))
}

private const val DEFAULT_BORDER_COLOR = "#FFFFFF"
