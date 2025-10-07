package app.eluvio.wallet.screens.nftdetail.legacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.FullscreenDialogStyle
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.title_62
import com.ramcosta.composedestinations.annotation.Destination


@Destination<MainGraph>(style = FullscreenDialogStyle::class)
@Composable
fun LockedMediaDialog(
    name: String,
    imageUrl: String,
    subtitle: String?,
    aspectRatio: Float,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(Overscan.defaultPadding())
    ) {
        ShimmerImage(
            model = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.title_62,
            modifier = Modifier.padding(16.dp)
        )
        subtitle?.let { Text(text = it, style = MaterialTheme.typography.body_32) }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun LockedMediaDialogPreview() = EluvioThemePreview {
    LockedMediaDialog(
        "Batarang AR (Locked)",
        "http://example.com/image.png",
        "Find this in the experience to unlock!",
        AspectRatio.SQUARE,
    )
}
