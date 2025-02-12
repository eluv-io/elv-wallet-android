package app.eluvio.wallet.screens.common

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
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.LocalSurfaceScale
import app.eluvio.wallet.theme.borders
import app.eluvio.wallet.theme.focusedBorder
import app.eluvio.wallet.util.compose.requestInitialFocus

/**
 * An image card with a focus border. Image is darkened when focused.
 */
@Composable
fun ImageCard(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    focusedOverlay: @Composable (BoxScope.() -> Unit)? = null,
    unFocusedOverlay: @Composable (BoxScope.() -> Unit)? = null,
    dimOnFocus: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        border = MaterialTheme.borders.focusedBorder,
        scale = LocalSurfaceScale.current,
        shape = ClickableSurfaceDefaults.shape(shape),
        interactionSource = interactionSource,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        modifier = modifier
    ) {
        val parentScope = this
        ShimmerImage(
            model = imageUrl,
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center)
                .dimContent(dim = dimOnFocus && isFocused)
        )
        if (isFocused) {
            focusedOverlay?.invoke(parentScope)
        } else {
            unFocusedOverlay?.invoke(parentScope)
        }
    }
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
