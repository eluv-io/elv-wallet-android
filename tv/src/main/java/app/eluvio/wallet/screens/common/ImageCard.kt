package app.eluvio.wallet.screens.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.eluvio.wallet.theme.borders
import app.eluvio.wallet.theme.focusedBorder
import app.eluvio.wallet.util.compose.Black
import app.eluvio.wallet.util.compose.LocalCardTheme
import app.eluvio.wallet.util.compose.cardBorder
import app.eluvio.wallet.util.compose.cardShape
import app.eluvio.wallet.util.compose.hasBorder
import app.eluvio.wallet.util.compose.requestInitialFocus

private val UnfocusedDim = Color.Black(alpha = 0.2f)
private val FocusedDim = Color.Black(alpha = 0.8f)

/** The web uses 0.5s, which drags when moving focus quickly along a row. */
private const val DIM_ANIMATION_MILLIS = 300

/**
 * An image card with a focus border. Image is slightly dimmed while unfocused, and heavily
 * darkened when focused (so [focusedOverlay] stays legible) unless [dimOnFocus] is false.
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
    dimOnFocus: Boolean = true,
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
    Surface(
        onClick = onClick,
        border = cardTheme.clickableSurfaceBorder(),
        scale = scale,
        shape = ClickableSurfaceDefaults.shape(cardTheme.cardShape(aspectRatio, shape)),
        interactionSource = interactionSource,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        modifier = modifier
    ) {
        val parentScope = this
        // Unfocused cards sit slightly dimmed so focus reads as "lit up", matching the web's 85%
        // inactive brightness. The much heavier focus dim is there to keep the overlay legible.
        val targetDim = when {
            alwaysDim -> FocusedDim
            !isFocused -> UnfocusedDim
            dimOnFocus -> FocusedDim
            else -> Color.Transparent
        }
        val dim by animateColorAsState(
            targetValue = targetDim,
            animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
            label = "cardDim"
        )
        ShimmerImage(
            model = imageUrl,
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center)
                .dimContent(color = dim)
        )
        if (isFocused) {
            focusedOverlay?.invoke(parentScope)
        } else {
            unFocusedOverlay?.invoke(parentScope)
        }
    }
}

/**
 * A theme that defines a border replaces the default focus ring with its own active/inactive
 * borders. Themes without a border keep the focus ring, so focus stays visible on TV.
 */
@Composable
private fun CardThemeEntity?.clickableSurfaceBorder(): ClickableSurfaceBorder {
    val theme = this?.takeIf { it.hasBorder } ?: return MaterialTheme.borders.focusedBorder
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
