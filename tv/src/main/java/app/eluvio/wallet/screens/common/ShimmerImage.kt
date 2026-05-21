package app.eluvio.wallet.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.color
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer

/**
 * An async image that shows a placeholder while loading.
 */
@Composable
fun ShimmerImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    var showPlaceholder by remember { mutableStateOf(true) }
    AsyncImage(
        model = model,
        onLoading = { showPlaceholder = true },
        onError = {
            onError?.invoke(it)
            showPlaceholder = false
        },
        onSuccess = {
            onSuccess?.invoke(it)
            showPlaceholder = false
        },
        modifier = modifier.placeholder(
            showPlaceholder,
            color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.secondaryContainer),
            highlight = PlaceholderHighlight.shimmer()
        ),
        contentDescription = contentDescription,
        contentScale = contentScale,
        alpha = alpha
    )
}
