package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.MetadataTexts
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.disabledItemAlpha
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.thenIf

@Composable
fun PageLinkCard(
    item: DynamicPageLayoutState.CarouselItem.PageLink,
    cardHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = item.displaySettings
    val title = display?.title
    val (imageUrl, imageAspectRatio) = display?.thumbnailUrlAndRatio ?: (null to null)
    Column(
        modifier
            .width(IntrinsicSize.Min)
            .thenIf(item.forceDisabled) {
                alpha(MaterialTheme.colorScheme.disabledItemAlpha)
            }
    ) {
        val cardModifier = Modifier
            .height(cardHeight)
            .aspectRatio(
                imageAspectRatio ?: AspectRatio.WIDE,
                matchHeightConstraintsFirst = true
            )
        if (item.forceDisabled) {
            ShimmerImage(
                imageUrl, contentDescription = title,
                modifier = cardModifier
                    .clip(MaterialTheme.shapes.medium)
            )
        } else {
            ImageCard(
                imageUrl = imageUrl,
                contentDescription = title,
                focusedOverlay = {
                    MetadataTexts(item.displaySettings)
                },
                onClick = onClick,
                modifier = cardModifier
            )
        }
        if (title != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.label_24.copy(fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun SubpropertyCardPreview(modifier: Modifier = Modifier) = EluvioThemePreview {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        PageLinkCard(
            item = DynamicPageLayoutState.CarouselItem.PageLink(
                permissionContext = PermissionContext(propertyId = "property1"),
                forceDisabled = false,
                propertyId = "subId",
                pageId = null,
                displaySettings = SimpleDisplaySettings(
                    forcedAspectRatio = AspectRatio.SQUARE,
                    subtitle = "subtitle",
                    title = "title",
                    headers = listOf("header1", "header2")
                ),
            ),
            cardHeight = 100.dp,
            onClick = {},
        )
    }
}
