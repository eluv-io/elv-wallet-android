package app.eluvio.wallet.screens.qrdialogs.generic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.callbackFor
import app.eluvio.wallet.screens.common.FullscreenDialogStyle
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.generateQrCodeBlocking
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>(
    style = FullscreenDialogStyle::class,
    navArgs = FullscreenQRDialogNavArgs::class
)
@Composable
fun FullscreenQRDialog() {
    hiltViewModel<FullscreenQRDialogViewModel>().subscribeToState { vm, state ->
        FullscreenQRDialog(state)
    }
}

@Composable
private fun FullscreenQRDialog(
    state: FullscreenQRDialogViewModel.State
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.title_62,
            modifier = Modifier.padding(bottom = 18.dp)
        )
        if (state.subtitle != null) {
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.label_40.copy(fontSize = 26.sp),
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
        Box(
            Modifier
                .padding(bottom = 27.dp)
                .heightIn(min = 250.dp)
        ) {
            if (state.qrImage != null) {
                Image(
                    bitmap = state.qrImage.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .height(250.dp)
                        .aspectRatio(1f)
                )
            }
        }
        TvButton(
            text = "Back",
            onClick = LocalNavigator.callbackFor(NavigationEvent.GoBack),
            modifier = Modifier.requestInitialFocus()
        )
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun FullscreenQRDialogPreview() = EluvioThemePreview {
    val url = "https://eluv.io"
    FullscreenQRDialog(
        FullscreenQRDialogViewModel.State(
            title = "Scan QR Code",
            subtitle = url,
            qrImage = generateQrCodeBlocking(url),
        )
    )
}
