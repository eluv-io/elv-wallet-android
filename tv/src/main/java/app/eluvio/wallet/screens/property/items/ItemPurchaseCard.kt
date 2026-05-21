package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.MetadataTexts
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.label_24

/**
 * A card that only uses [DisplaySettings], and doesn't have any special behavior.
 */
@Composable
fun DisplaySettingsCard(
    displaySettings: DisplaySettings?,
    cardHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = displaySettings?.title
    val (imageUrl, imageAspectRatio) = displaySettings?.thumbnailUrlAndRatio ?: (null to null)
    Column(modifier = modifier.width(IntrinsicSize.Min)) {
        ImageCard(
            imageUrl = imageUrl,
            contentDescription = title,
            focusedOverlay = {
                MetadataTexts(displaySettings)
            },
            onClick = onClick,
            modifier = Modifier
                .height(cardHeight)
                .aspectRatio(
                    imageAspectRatio ?: AspectRatio.WIDE,
                    matchHeightConstraintsFirst = true
                )
        )
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
private fun ItemPurchaseCardPreview() = EluvioThemePreview {
    DisplaySettingsCard(
        displaySettings = SimpleDisplaySettings(
            title = "Title that is really really really really really really really really long",
            forcedAspectRatio = AspectRatio.SQUARE
        ),
        cardHeight = 150.dp,
        onClick = {}
    )
}
