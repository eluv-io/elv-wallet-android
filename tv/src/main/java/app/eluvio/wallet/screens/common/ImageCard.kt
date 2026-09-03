package app.eluvio.wallet.screens.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.LocalSurfaceScale
import app.eluvio.wallet.util.compose.Black
import app.eluvio.wallet.util.compose.LocalCardTheme
import app.eluvio.wallet.util.compose.cardBackground
import app.eluvio.wallet.util.compose.cardBorder
import app.eluvio.wallet.util.compose.cardShape
import app.eluvio.wallet.util.compose.hasBorder
import app.eluvio.wallet.util.compose.imageSaturation
import app.eluvio.wallet.util.compose.saturationFilter
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.compose.toBrush

private val UnfocusedDim = Color.Black(alpha = 0.2f)

/**
 * Inaccessible content keeps a flat dim (and a stroke) so the "view purchase options" CTA reads
 * over any image, rather than the focus treatment below.
 */
private val UnauthorizedDim = Color.Black(alpha = 0.7f)
private val UnauthorizedStroke = Color(0xFF777777)

/** Focus lights the card up along its top edge... */
private val SheenBrush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.45f),
    0.16f to Color.White.copy(alpha = 0.16f),
    0.42f to Color.Transparent,
)

/** ...and darkens the bottom third, which is where [ImageCard]'s overlays put their text. */
private val FocusScrimBrush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.62f to Color.Black(alpha = 0.6f),
    1f to Color.Black(alpha = 0.92f),
)
private const val FOCUS_SCRIM_HEIGHT = 0.64f

/** The web uses 0.5s, which drags when moving focus quickly along a row. */
const val DIM_ANIMATION_MILLIS = 300

/**
 * An image card with a focus ring. The image sits slightly dimmed while unfocused; focusing it
 * lifts the dim and adds a top sheen plus a bottom scrim that keeps [focusedOverlay] legible,
 * unless [respondToFocus] is false.
 */
@Composable
fun ImageCard(
    // Any? rather than String? so callers can pass a FabricUrl directly (which lets Coil's
    // FabricImageInterceptor apply a ThumbHash placeholder) without losing the typed wrapper.
    imageUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    focusedOverlay: @Composable (BoxScope.() -> Unit)? = null,
    unFocusedOverlay: @Composable (BoxScope.() -> Unit)? = null,
    respondToFocus: Boolean = true,
    /**
     * Keeps the card fully dimmed regardless of focus, for content the user can't access.
     * Has to be part of the dim itself rather than an overlay, otherwise it would pop in and out
     * on focus changes while the dim is still animating.
     */
    alwaysDim: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    /** Lets the card theme decide whether this card should be circularized. */
    aspectRatio: Float? = null,
    onClick: () -> Unit,
    scale: ClickableSurfaceScale = LocalSurfaceScale.current,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val cardTheme = LocalCardTheme.current
    val cardShape = cardTheme.cardShape(aspectRatio, shape)
    Surface(
        onClick = onClick,
        border = cardTheme.clickableSurfaceBorder(),
        scale = scale,
        shape = ClickableSurfaceDefaults.shape(cardShape),
        interactionSource = interactionSource,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        modifier = modifier
    ) {
        val parentScope = this
        // Unfocused cards sit slightly dimmed so focus reads as "lit up", matching the web's 85%
        // inactive brightness.
        val targetDim = when {
            alwaysDim -> UnauthorizedDim
            !isFocused -> UnfocusedDim
            else -> Color.Transparent
        }
        val dim by animateColorAsState(
            targetValue = targetDim,
            animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
            label = "cardDim"
        )
        val background = cardTheme.cardBackground(focused = isFocused)
        if (background != null) {
            // Cross-faded with focus, like the dim, so the two states don't fight each other.
            // It isn't dimmed itself - the theme already says what each state should look like.
            val startColor by animateColorAsState(
                targetValue = background.startColor,
                animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
                label = "cardBackgroundStart"
            )
            val endColor by animateColorAsState(
                targetValue = background.endColor,
                animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
                label = "cardBackgroundEnd"
            )
            Spacer(
                Modifier
                    .matchParentSize()
                    .background(
                        background.copy(startColor = startColor, endColor = endColor).toBrush()
                    )
            )
        }
        // Themes can grey out unfocused cards. Animated alongside the dim, so color washes
        // back in as the card lights up.
        val saturation by animateFloatAsState(
            targetValue = cardTheme.imageSaturation(focused = isFocused),
            animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
            label = "cardSaturation"
        )
        ShimmerImage(
            model = imageUrl,
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
            colorFilter = saturationFilter(saturation),
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center)
                .dimContent(color = dim)
        )
        // Drawn under the overlays, so their text sits on top of the scrim.
        val focusTreatment = respondToFocus && !alwaysDim
        val treatment by animateFloatAsState(
            targetValue = if (isFocused && focusTreatment) 1f else 0f,
            animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
            label = "cardFocusTreatment"
        )
        if (treatment > 0f) {
            Spacer(
                Modifier
                    .matchParentSize()
                    .alpha(treatment)
                    .background(SheenBrush)
            )
            Spacer(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(FOCUS_SCRIM_HEIGHT)
                    .alpha(treatment)
                    .background(FocusScrimBrush)
            )
        }
        if (isFocused) {
            focusedOverlay?.invoke(parentScope)
        } else {
            unFocusedOverlay?.invoke(parentScope)
        }
        if (alwaysDim) {
            Spacer(Modifier.matchParentSize().border(2.dp, UnauthorizedStroke, cardShape))
        }
        if (isFocused && !cardTheme.hasBorder) {
            AnimatedFocusRing(cardShape)
        }
    }
}

/**
 * A theme that defines a border replaces the focus treatment with its own active/inactive
 * borders. Without one the Surface draws no border at all, and focus is shown with the same
 * [AnimatedFocusRing] the Discover tiles use.
 */
@Composable
private fun CardThemeEntity?.clickableSurfaceBorder(): ClickableSurfaceBorder {
    val theme = this?.takeIf { it.hasBorder } ?: return ClickableSurfaceDefaults.border()
    return ClickableSurfaceDefaults.border(
        border = Border(theme.cardBorder(focused = false)),
        focusedBorder = Border(theme.cardBorder(focused = true)),
    )
}

@Preview(widthDp = 300, heightDp = 150)
@Composable
private fun ImageCardPreview() = EluvioThemePreview {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        ImageCard(
            imageUrl = "",
            contentDescription = "Card Title",
            onClick = {},
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        ImageCard(
            imageUrl = "",
            contentDescription = "Card Title",
            onClick = {},
            modifier = Modifier
                .size(120.dp)
                .requestInitialFocus()
        )
    }
}
