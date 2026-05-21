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
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.title_62


@Composable
fun LockedMediaDialog(args: LockedMediaDialogNavArgs) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(Overscan.defaultPadding())
    ) {
        ShimmerImage(
            model = args.imageUrl,
            contentDescription = args.name,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(args.aspectRatio, matchHeightConstraintsFirst = true)
        )
        Text(
            text = args.name,
            style = MaterialTheme.typography.title_62,
            modifier = Modifier.padding(16.dp)
        )
        args.subtitle?.let { Text(text = it, style = MaterialTheme.typography.body_32) }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun LockedMediaDialogPreview() = EluvioThemePreview {
    LockedMediaDialog(
        LockedMediaDialogNavArgs(
            name = "Batarang AR (Locked)",
            imageUrl = "http://example.com/image.png",
            subtitle = "Find this in the experience to unlock!",
            aspectRatio = AspectRatio.SQUARE,
        )
    )
}
