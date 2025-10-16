package app.eluvio.wallet.screens.signin

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.generateQrCodeBlocking
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.RealisticDevices
import app.eluvio.wallet.util.compose.requestOnce
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination


@Destination<MainGraph>(navArgs = SignInNavArgs::class)
@Composable
fun SignIn() {
    hiltViewModel<SignInViewModel>().subscribeToState { vm, state ->
        SignInView(state, onRequestNewToken = vm::requestNewToken)
    }
}

@Composable
fun SignInView(state: SignInViewModel.State, onRequestNewToken: () -> Unit) {
    AsyncImage(
        model = state.bgImageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.7f,
        modifier = Modifier.fillMaxSize()
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 37.dp)
    ) {

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Sign In",
            style = MaterialTheme.typography.title_62,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp)
        ) {
            if (state.loading) {
                EluvioLoadingSpinner()
            } else {
                QrData(state.qrCode, state.userCode)
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Column(Modifier.width(IntrinsicSize.Max)) {
            Row {
                val focusRequester = remember { FocusRequester() }
                TvButton(
                    stringResource(R.string.request_new_code),
                    onClick = onRequestNewToken,
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 5.dp),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                focusRequester.requestOnce()
                Spacer(modifier = Modifier.width(10.dp))
                val navigator = LocalNavigator.current
                TvButton(
                    text = "Back",
                    onClick = { navigator(NavigationEvent.GoBack) })
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QrData(qrCode: Bitmap?, userCode: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = userCode ?: "",
            style = MaterialTheme.typography.title_62
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (qrCode != null) {
            Image(
                bitmap = qrCode.asImageBitmap(),
                contentDescription = "qr code",
            )
        }
    }
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun SignInViewPreview() = EluvioThemePreview {
    SignInView(
        SignInViewModel.State(
            loading = false,
            qrCode = generateQrCodeBlocking("https://eluv.io/?code=1234567890"),
            userCode = "ABCDEF",
        ),
        onRequestNewToken = {},
    )
}
