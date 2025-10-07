package app.eluvio.wallet.screens.qrdialogs.fulfillment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.generateQrCodeBlocking
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.carousel_48
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@Destination<MainGraph>(navArgs = FulfillmentQrDialogNavArgs::class)
@Composable
fun FulfillmentQrDialog() {
    hiltViewModel<FulfillmentQrDialogViewModel>().subscribeToState { vm, state ->
        FulfillmentQrDialog(state)
    }
}

@Composable
private fun FulfillmentQrDialog(state: FulfillmentQrDialogViewModel.State) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(Overscan.defaultPadding())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight()
                .align(Alignment.Center)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Success", style = MaterialTheme.typography.title_62)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scan the QR Code with your camera app or a QR code reader on your device to claim your reward.",
                style = MaterialTheme.typography.label_40,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (state.loading) {
                Spacer(modifier = Modifier.weight(1f))
                EluvioLoadingSpinner()
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Text(text = state.code, style = MaterialTheme.typography.carousel_48)
                Spacer(modifier = Modifier.height(6.dp))
                if (state.qrBitmap != null) {
                    Image(
                        bitmap = state.qrBitmap.asImageBitmap(),
                        contentDescription = "qr code",
                        Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(34.dp))
            val navigator = LocalNavigator.current
            TvButton(
                text = "Back",
                onClick = { navigator(NavigationEvent.GoBack) },
                Modifier.requestInitialFocus()
            )
            Spacer(modifier = Modifier.heightIn(min = 16.dp, max = 32.dp))
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun FulfillmentQrDialogLoadingPreview() = EluvioThemePreview {
    FulfillmentQrDialog(FulfillmentQrDialogViewModel.State(loading = true))
}

@Composable
@Preview(device = Devices.TV_720p)
private fun FulfillmentQrDialogPreview() = EluvioThemePreview {
    FulfillmentQrDialog(
        FulfillmentQrDialogViewModel.State(
            loading = false,
            code = "1234567890",
            qrBitmap = generateQrCodeBlocking("https://eluv.io/?code=1234567890")
        )
    )
}
