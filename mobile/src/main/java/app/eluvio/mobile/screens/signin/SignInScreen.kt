package app.eluvio.mobile.screens.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eluvio.mobile.R
import app.eluvio.wallet.util.subscribeToState

/**
 * Mobile sign-in screen. A translucent fullscreen-dialog overlay rendered above the previous
 * back-stack entry (window-level concerns — dialog dim, animation suppression, enter/exit fade —
 * are handled by [app.eluvio.mobile.navigation.FadeDialogSceneStrategy]).
 *
 * It renders a scrim + spinner and drives a [SignInFlow]: once the activation URL is ready it
 * launches the flow, and the flow's [SignInResult] is handed back to
 * [MobileSignInViewModel.onSignInResult]. All the per-flow launch/capture/cancel logic lives in
 * [rememberSignInFlow]; this screen only owns the "launch once" guard and the UI.
 */
@Composable
internal fun SignInScreen(vm: MobileSignInViewModel) {
    val flow = rememberSignInFlow(onResult = vm::onSignInResult)
    vm.subscribeToState { _, state ->
        var launchedUrl by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(state.signInUrl, state.loadingContent) {
            val url = state.signInUrl
            if (!state.loadingContent && url != null && url != launchedUrl) {
                launchedUrl = url
                flow.launch(url)
            }
        }
        SignInOverlay(loadingContent = state.loadingContent)
    }
}

@Composable
private fun SignInOverlay(loadingContent: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            // Only after the redirect is captured with a token — the post-auth prefetch
            // is in flight, so it's worth signaling that we're not stuck.
            if (loadingContent) {
                OutlinedText(text = stringResource(R.string.sign_in_almost_done))
            }
        }
    }
}

@Composable
private fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    outlineWidth: Dp = 1.dp,
) {
    val strokePx = with(LocalDensity.current) { outlineWidth.toPx() }
    Box(modifier) {
        // Stroke pass — drawn behind the fill so the outline hugs the glyphs.
        Text(
            text,
            style = style.copy(
                color = Color.Black,
                drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round),
            ),
        )
        Text(text, style = style)
    }
}

private val SCRIM_COLOR = Color(0x99000000)
