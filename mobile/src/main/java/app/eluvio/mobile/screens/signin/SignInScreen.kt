package app.eluvio.mobile.screens.signin

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.core.net.toUri
import app.eluvio.mobile.R
import app.eluvio.wallet.util.logging.Log

/**
 * Translucent overlay rendered as a fullscreen dialog above the previous back-stack entry.
 * Window-level concerns (dialog dim, platform animation suppression, enter/exit fade) are
 * handled by [app.eluvio.mobile.navigation.FadeDialogSceneStrategy] — this screen only
 * renders the scrim + spinner and hosts the Auth Tab launcher.
 *
 * Completion is redirect-driven: the wallet web app is launched with `&response=redirect&
 * redirect=elvwallet://auth-complete`, so on success it hard-redirects to that URL with
 * `?elvToken=<token>` appended. The Auth Tab matches [AUTH_REDIRECT_SCHEME] and closes,
 * delivering the URI to [onAuthCaptured]. If the user dismisses the tab without completing
 * auth, [onAuthTabDismissed] fires instead.
 */
@Composable
fun SignInScreen(
    signInUrl: String?,
    loadingContent: Boolean,
    onAuthCaptured: (Uri) -> Unit,
    onAuthTabDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        AuthTabIntent.AuthenticateUserResultContract()
    ) { result ->
        Log.i("Auth Tab closed with resultCode=${result.resultCode}")
        val uri = result.resultUri
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            Log.i("Auth Tab captured callback uri=$uri")
            onAuthCaptured(uri)
        } else {
            onAuthTabDismissed()
        }
    }

    var launchedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(signInUrl, loadingContent) {
        if (!loadingContent && signInUrl != null && signInUrl != launchedUrl) {
            launchedUrl = signInUrl
            Log.d("Launching Auth Tab for url=$signInUrl")
            AuthTabIntent.Builder()
                .setEphemeralBrowsingEnabled(false)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                .build()
                .launch(launcher, signInUrl.toUri(), AUTH_REDIRECT_SCHEME)
        }
    }

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
            // Only after the Auth Tab returns with a captured token — the post-auth prefetch
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

/**
 * Custom scheme handed to the Auth Tab. The wallet web app redirects to
 * `elvwallet://auth-complete?elvToken=<token>` on successful sign-in; Auth Tab matches the
 * scheme and closes, delivering the URI as the activity result.
 */
private const val AUTH_REDIRECT_SCHEME = "elvwallet"

private val SCRIM_COLOR = Color(0x99000000)
