package app.eluvio.wallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.util.Consumer
import app.eluvio.wallet.navigation.ComposeNavigator
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainNavHost
import app.eluvio.wallet.screens.home.DeeplinkArgs
import app.eluvio.wallet.theme.EluvioTheme
import app.eluvio.wallet.util.logging.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Marker typealias for the main activity on TV, to make it easy to search in IDE.
 * Renaming a launcher activity is non-trivial, so this is an easy compromise.
 */
typealias MainTvActivity = MainActivity

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EluvioTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painterResource(id = R.drawable.bg_gradient),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                    val backStack = rememberNavBackStack(parseDeeplink(intent) ?: DeeplinkArgs())
                    DisposableEffect(backStack) {
                        val consumer = Consumer<Intent> { newIntent ->
                            val args = parseDeeplink(newIntent) ?: return@Consumer
                            Log.d("New deeplink intent received, pushing to backStack: $args")
                            // Reset to the deeplink target — matches Nav 2's handleDeepLink behavior
                            // of replacing the stack with the deeplinked entry as the root.
                            backStack.clear()
                            backStack.add(args)
                        }
                        addOnNewIntentListener(consumer)
                        onDispose { removeOnNewIntentListener(consumer) }
                    }
                    firebaseScreenTracking(backStack, this@MainActivity)
                    if (BuildConfig.DEBUG) backStack.printOnChange()

                    val navigator = remember {
                        ComposeNavigator(
                            backStack = backStack,
                            context = this@MainActivity,
                        )
                    }
                    CompositionLocalProvider(LocalNavigator provides navigator) {
                        MainNavHost(
                            backStack = backStack,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private val envSelectorHook by lazy { EnvSelectorHook(this) }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (BuildConfig.DEBUG && envSelectorHook.onKeyUp(keyCode, event)) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }
}

/**
 * Parses an `elvwallet://{action}/{marketplace}/{contract}/{sku}?jwt=...&entitlement=...&back_link=...`
 * deeplink intent into [DeeplinkArgs]. Returns null if the intent has no data or the scheme
 * doesn't match — typical for plain launcher launches.
 *
 * Replaces Nav 2's `navDeepLink { uriPattern = "elvwallet://..." }` matching.
 */
private fun parseDeeplink(intent: Intent?): DeeplinkArgs? {
    val data: Uri = intent?.data?.takeIf { it.scheme == "elvwallet" } ?: return null
    val segments = data.pathSegments
    return DeeplinkArgs(
        action = data.host,
        marketplace = segments.getOrNull(0),
        contract = segments.getOrNull(1),
        sku = segments.getOrNull(2),
        jwt = data.getQueryParameter("jwt"),
        entitlement = data.getQueryParameter("entitlement"),
        backLink = data.getQueryParameter("back_link"),
    )
}

@Composable
private fun firebaseScreenTracking(backStack: NavBackStack<NavKey>, context: Context) {
    // Builds without google-services.json don't apply the google-services Gradle plugin, so
    // Firebase is never auto-initialized and FirebaseAnalytics.getInstance() would throw.
    val analytics = remember {
        FirebaseApp.getApps(context).firstOrNull()
            ?.let { FirebaseAnalytics.getInstance(context) }
    } ?: return
    LaunchedEffect(backStack) {
        snapshotFlow { backStack.lastOrNull() }
            .collect { current ->
                val screenName = current?.let { it::class.simpleName } ?: return@collect
                analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
                }
            }
    }
}

@Composable
private fun NavBackStack<NavKey>.printOnChange(prefix: String = "navstack") {
    LaunchedEffect(this) {
        snapshotFlow { toList() }.collect { entries ->
            Log.v("$prefix = ${entries.map { it::class.simpleName ?: it.toString() }}")
        }
    }
}

private class EnvSelectorHook(private val context: Context) {
    private val magicSequence = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    private var index = 0

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            if (keyCode == magicSequence[index]) {
                index++
                if (index == magicSequence.size) {
                    index = 0
                    // Sequence completed. Launching the debug activity
                    context.startActivity(Intent().apply {
                        component = ComponentName(
                            context.packageName,
                            "app.eluvio.wallet.debug.EnvSelectActivity"
                        )
                    })
                    return true
                }
            } else {
                index = 0
            }
        }
        return false
    }
}
