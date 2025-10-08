package app.eluvio.wallet.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.requestOnce
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@Destination<MainGraph>(navArgs = ImageGalleryNavArgs::class)
@Composable
fun ImageGallery() {
    hiltViewModel<ImageGalleryViewModel>().subscribeToState { vm, state ->
        ImageGallery(state)
    }
}

@Composable
private fun ImageGallery(state: ImageGalleryViewModel.State) {
    var selectedImage by remember { mutableStateOf(state.images.firstOrNull()) }
    selectedImage?.let { image ->
        ShimmerImage(
            model = image.url,
            contentDescription = image.name,
            Modifier.fillMaxSize()
        )
        if (state.images.size == 1 && !image.name.isNullOrEmpty()) {
            // Only show caption in "single image" mode.
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.title_62.copy(fontSize = 22.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xB2000000))
                        .padding(vertical = 22.dp, horizontal = 80.dp)
                )
            }

        }
    }
    var focusRequestedOnce by remember { mutableStateOf(false) }

    LazyRow(
        contentPadding = PaddingValues(32.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        itemsIndexed(state.images) { index, image ->
            var isFocused by remember { mutableStateOf(false) }
            if (isFocused) {
                selectedImage = image
            }
            val focusRequester = remember { FocusRequester() }
            Surface(
                onClick = { /*TODO*/ },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.hasFocus }
                    .height(100.dp)
                    .then(
                        image.aspectRatio?.let {
                            Modifier.aspectRatio(it, matchHeightConstraintsFirst = true)
                        } ?: Modifier
                    )
            ) {
                // Don't show thumbnails when there's only 1 item in the gallery
                if (state.images.size > 1) {
                    val imageAlpha by remember { derivedStateOf { if (isFocused) 1f else 0.75f } }
                    ShimmerImage(
                        model = image.url,
                        contentDescription = image.name,
                        alpha = imageAlpha,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                    )
                }
            }
            if (!focusRequestedOnce && index == 0) {
                focusRequestedOnce = true
                focusRequester.requestOnce()
            }
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun ImageGalleryPreview() = EluvioThemePreview {
    ImageGallery(
        ImageGalleryViewModel.State(
            listOf(
                ImageGalleryViewModel.State.GalleryImage(
                    url = "",
                    name = ""
                ),
                ImageGalleryViewModel.State.GalleryImage(
                    url = "",
                    name = ""
                ),
                ImageGalleryViewModel.State.GalleryImage(
                    url = "",
                    name = ""
                ),
            )
        )
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun SingleImageNoTitlePreview() = EluvioThemePreview {
    ImageGallery(
        ImageGalleryViewModel.State(
            listOf(
                ImageGalleryViewModel.State.GalleryImage(
                    url = "",
                    name = ""
                ),
            )
        )
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun SingleImageWithTitlePreview() = EluvioThemePreview {
    ImageGallery(
        ImageGalleryViewModel.State(
            listOf(
                ImageGalleryViewModel.State.GalleryImage(
                    url = "",
                    name = "This is a caption for the photo"
                ),
            )
        )
    )
}