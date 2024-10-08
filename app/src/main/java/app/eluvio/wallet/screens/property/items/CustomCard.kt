package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.WrapContentText
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.theme.title_62

@Composable
fun CustomCard(item: CarouselItem.CustomCard, cardHeight: Dp, modifier: Modifier) {
    ImageCard(
        imageUrl = item.imageUrl?.url,
        contentDescription = item.title,
        onClick = item.onClick,
        modifier = modifier
            .height(cardHeight)
            .aspectRatio(item.aspectRatio),
        focusedOverlay = {
            if (item.imageUrl == null) {
                CustomCardOverlay(text = item.title)
            }
        },
        unFocusedOverlay = {
            if (item.imageUrl == null) {
                CustomCardOverlay(text = item.title)
            }
        },
        dimOnFocus = false
    )
}

@Composable
private fun CustomCardOverlay(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(overlayBackground) then modifier
    ) {
        WrapContentText(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title_62.copy(fontSize = 25.sp),
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 20.dp)
        )
    }
}

private val overlayBackground = Brush.verticalGradient(
    listOf(Color(0xFF2E2E2E), Color(0xFF3D3D3D))
)
