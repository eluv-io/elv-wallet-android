package app.eluvio.wallet.screens.dashboard.myitems

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.LocalSurfaceScale
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.theme.label_24


/**
 * @param onClick if null, the card will not be clickable/focusable/interactive
 */
@Composable
fun MediaCard(
    media: AllMediaProvider.Media,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    @DrawableRes val bgResource: Int = remember(focused) {
        if (focused) R.drawable.item_card_bg_focused else R.drawable.item_card_bg
    }
    SurfaceWrapper(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.aspectRatio(0.65f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                // TODO: convert to drawWithCache and a gradient brush?
                //red: https://github.com/HedvigInsurance/android/blob/cc66f77f617c8ddf09790f8a0d50383febfbb1ff/app/app/src/main/kotlin/com/hedvig/app/feature/loggedin/ui/LoggedInActivity.kt#L297C27-L297C49
                .paint(
                    painterResource(id = bgResource),
                    contentScale = ContentScale.FillBounds
                )
                .padding(12.dp)
        ) {
            Header(media)
            Spacer(Modifier.height(14.dp))
            ShimmerImage(
                model = media.imageUrl,
                contentDescription = "NFT image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(2.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = media.title,
                color = if (focused) LocalContentColor.current else titleUnfocusedColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.carousel_36
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (media.tokenCount > 1) {
                Text(
                    text = "View all ${media.tokenCount}",
                    style = MaterialTheme.typography.label_24,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        .padding(vertical = 2.dp, horizontal = 8.dp)
                )
            } else {
                media.subtitle?.let {
                    Text(
                        text = it.uppercase(), style = MaterialTheme.typography.label_24,
                        color = if (focused) subtitleFocusedColor else LocalContentColor.current
                    )
                }
            }
        }
    }
}

/**
 * [Surface] has interactive, and non-interactive variants. This wrapper will choose a variant
 * based on whether [onClick] is null.
 */
@Composable
private fun SurfaceWrapper(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            scale = LocalSurfaceScale.current,
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF7A7A7A),
                focusedContainerColor = Color.Transparent,
                focusedContentColor = Color.Black,
                pressedContentColor = Color.Black,
            ),
            modifier = modifier.aspectRatio(0.65f),
            content = content
        )
    } else {
        Surface(
            colors = SurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF7A7A7A),
            ),
            modifier = modifier.aspectRatio(0.65f),
            content = content
        )
    }
}

@Composable
private fun Header(media: AllMediaProvider.Media) {
    Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
        // add logo
        // add marketplace name
        Spacer(modifier = Modifier.weight(1f))
        media.tokenId?.let {
            Text(
                text = "#$it",
                style = MaterialTheme.typography.label_24
            )
        }
    }
}

private val titleUnfocusedColor = Color.White
private val subtitleFocusedColor = Color(0xFF646464)

@Composable
@Preview
fun MediaCardPreviewPack() = EluvioThemePreview {
    MediaCard(
        AllMediaProvider.Media(
            key = "key",
            contractAddress = "id1",
            imageUrl = "https://x",
            title = "Goat Pack",
            subtitle = "Special Edition",
            description = "desc",
            tokenId = null,
            tokenCount = 23
        ),
        onClick = {},
    )
}

@Composable
@Preview
fun MediaCardPreviewSingle() = EluvioThemePreview {
    MediaCard(
        AllMediaProvider.Media(
            key = "key",
            contractAddress = "id1",
            imageUrl = "https://x",
            title = "Single Token",
            subtitle = "Special Edition",
            description = "desc",
            tokenId = "1",
            tokenCount = 1
        ),
        onClick = {},
    )
}
