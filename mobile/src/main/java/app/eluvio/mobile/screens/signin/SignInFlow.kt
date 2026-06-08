package app.eluvio.mobile.screens.signin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.eluvio.wallet.util.logging.Log

/**
 * A way to run the sign-in web flow. Each implementation owns the whole per-flow lifecycle —
 * how it launches, how the `elvauth://` redirect is captured, and how cancellation is detected —
 * and funnels the result into the `onResult` it was created with. See [rememberSignInFlow].
 */
fun interface SignInFlow {
    fun launch(url: String)
}

/** Outcome of a sign-in flow, regardless of which browser mechanism produced it. */
sealed interface SignInResult {
    /** The wallet redirected back with the auth payload (`?elvToken=…`). */
    data class Captured(val uri: Uri) : SignInResult

    /** The user dismissed the flow, or there was no browser to launch it in. */
    data object Cancelled : SignInResult
}

/**
 * Picks and wires the sign-in flow for this device, always deferring to the user's *default*
 * browser (shared session, autofill, expected UX) rather than hunting for any tab-capable one:
 * - default browser implements Auth Tab → [rememberAuthTabFlow]
 * - otherwise → [rememberBrowserFlow] (Custom Tab, degrading to a plain browser)
 *
 * Only the selected flow is wired — the other's launcher / deep-link + resume hooks are never
 * created. Conditionally calling one `remember*Flow` is safe because the choice derives from the
 * default browser and its capabilities, which are fixed for this screen's lifetime: the branch
 * never flips, so Compose keeps the flow's state across recompositions.
 */
@Composable
fun rememberSignInFlow(
    onResult: (SignInResult) -> Unit,
): SignInFlow {
    val context = LocalContext.current
    val browser = remember { defaultBrowserPackage(context) }
    val useAuthTab = remember(browser) {
        browser != null && CustomTabsClient.isAuthTabSupported(context, browser)
    }
    return if (useAuthTab) {
        rememberAuthTabFlow(browser, onResult)
    } else {
        rememberBrowserFlow(browser, onResult)
    }
}

/**
 * Auth Tab flow: launches an [AuthTabIntent], handled by the user's default browser — already
 * verified to support Auth Tab, so the unpinned `VIEW` intent resolves to it. The browser
 * intercepts the `elvauth://` redirect and returns it as an activity result, so both capture and
 * cancellation arrive through the launcher — no deep link or resume-watching needed.
 */
@Composable
private fun rememberAuthTabFlow(
    browserPackage: String?,
    onResult: (SignInResult) -> Unit,
): SignInFlow {
    val launcher = rememberLauncherForActivityResult(
        AuthTabIntent.AuthenticateUserResultContract()
    ) { result ->
        Log.i("Auth Tab closed with resultCode=${result.resultCode}")
        val uri = result.resultUri
        onResult(
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                SignInResult.Captured(uri)
            } else {
                SignInResult.Cancelled
            }
        )
    }
    return remember(browserPackage, launcher) {
        SignInFlow { url ->
            Log.d("Launching Auth Tab in $browserPackage for url=$url")
            AuthTabIntent.Builder()
                .setEphemeralBrowsingEnabled(false)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                .build()
                .launch(launcher, url.toUri(), AuthRedirect.SCHEME)
        }
    }
}

/**
 * Custom Tab / browser flow: launches the URL in the user's default browser (degrading to a plain
 * browser when it has no Custom Tabs service). A Custom Tab returns nothing, so:
 * - capture comes from the `elvauth://` redirect arriving as a `VIEW` intent on the hosting
 *   activity (`onNewIntent`), observed here via [ComponentActivity.addOnNewIntentListener], and
 * - cancellation is inferred when we resume to the foreground while still `awaiting` a redirect
 *   (the user backed out). `onNewIntent` runs before `ON_RESUME`, so a successful redirect clears
 *   `awaiting` first; the initial resume (before launch) is harmless because `awaiting` is only
 *   armed by [SignInFlow.launch].
 */
@Composable
private fun rememberBrowserFlow(
    browserPackage: String?,
    onResult: (SignInResult) -> Unit,
): SignInFlow {
    val context = LocalContext.current
    var awaiting by remember { mutableStateOf(false) }

    CaptureAuthRedirectIntents { uri ->
        awaiting = false
        onResult(SignInResult.Captured(uri))
    }

    DetectResumeWithoutRedirect(
        isAwaiting = { awaiting },
        onCancelled = {
            awaiting = false
            onResult(SignInResult.Cancelled)
        },
    )

    return remember(browserPackage) {
        SignInFlow { url ->
            try {
                Log.d("Launching Custom Tab in ${browserPackage ?: "system default"} for url=$url")
                CustomTabsIntent.Builder()
                    .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                    .build()
                    .launchUrl(context, url.toUri())
                awaiting = true
            } catch (e: ActivityNotFoundException) {
                Log.w("No browser available to handle sign-in", e)
                onResult(SignInResult.Cancelled)
            }
        }
    }
}

/**
 * Observes the `elvauth://` redirect delivered to the hosting activity as a `VIEW` intent
 * (`onNewIntent`) and forwards its URI to [onCaptured]. No-op when there's no [ComponentActivity]
 * host to receive the intent.
 */
@Composable
private fun CaptureAuthRedirectIntents(onCaptured: (Uri) -> Unit) {
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        activity ?: return@DisposableEffect run {
            Log.w("No ComponentActivity host; auth redirect deep link can't be captured")
            onDispose { }
        }
        val consumer = Consumer<Intent> { intent ->
            val uri = intent.data
            if (uri != null && uri.scheme == AuthRedirect.SCHEME) {
                onCaptured(uri)
            }
        }
        activity.addOnNewIntentListener(consumer)
        onDispose { activity.removeOnNewIntentListener(consumer) }
    }
}

/**
 * Infers cancellation: if we return to the foreground (`ON_RESUME`) while still [isAwaiting] a
 * redirect, the user backed out of the browser without completing sign-in, so [onCancelled] fires.
 * A successful redirect arrives via `onNewIntent` before `ON_RESUME`, so by then [isAwaiting] is
 * already false.
 */
@Composable
private fun DetectResumeWithoutRedirect(
    isAwaiting: () -> Boolean,
    onCancelled: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isAwaiting()) {
                Log.d("Resumed from browser without a redirect — cancelling sign-in")
                onCancelled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

/**
 * The user's default browser package, or null if there's no single default (none installed, or
 * the system would show a chooser). Resolved via the generic web intent — visible without a
 * `<queries>` entry — rather than [CustomTabsClient.getPackageName], which falls back to a
 * *non-default* Custom Tabs provider when the default lacks one.
 */
private fun defaultBrowserPackage(context: Context): String? =
    context.packageManager
        .resolveActivity(
            Intent(Intent.ACTION_VIEW, "https://".toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        ?.activityInfo
        ?.packageName
        // The resolver/chooser activity ("android") isn't a real browser to pin to.
        ?.takeUnless { it == "android" }
