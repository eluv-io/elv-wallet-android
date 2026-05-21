package app.eluvio.mobile.screens.signin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import app.eluvio.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    signInUrl: String?,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_in_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SignInWebView(signInUrl = signInUrl, modifier = Modifier.fillMaxSize())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SignInWebView(
    signInUrl: String?,
    modifier: Modifier = Modifier,
) {
    // Tracks the URL we last asked the WebView to load. Used to dedupe redundant loadUrl
    // calls across recompositions, which would otherwise wipe in-progress form input (email,
    // password, etc.) whenever the VM emits an unrelated state tick (e.g. a new userCode
    // alongside the same signInUrl).
    //
    // Plain class — *not* MutableState — because nothing observes this value. Using
    // MutableState would invite a future reader to subscribe from a composable, silently
    // turning every URL change into an extra recomposition. We can't use `webView.url`
    // either: it drifts as the page navigates through auth-provider redirects, so it
    // doesn't reliably represent "the URL we asked to load".
    val lastLoaded = remember { LoadedUrl() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Without a WebViewClient, every navigation goes through Android's intent
                // dispatcher, which blocks programmatic cross-origin redirects with "Denied
                // starting an intent without a user gesture". Setting any WebViewClient keeps
                // loads inside this view — the auth provider redirect (e.g. Auth0) needs this.
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            if (signInUrl != null && signInUrl != lastLoaded.url) {
                lastLoaded.url = signInUrl
                webView.loadUrl(signInUrl)
            }
        },
        // Two things happen on leaving composition:
        //  1. `destroy()` — WebView holds heavy native state (JS engine, rendering thread)
        //     that view teardown alone won't release.
        //  2. Cookie + localStorage wipe — the auth provider's session cookies persist
        //     across our app-level sign-outs and would silently re-auth the same account
        //     on the next entry. We don't need them after this screen exits: the app
        //     exchanges the auth code for a token via the API client, not the WebView.
        onRelease = { webView ->
            webView.destroy()
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        },
    )
}

private class LoadedUrl {
    var url: String? = null
}
