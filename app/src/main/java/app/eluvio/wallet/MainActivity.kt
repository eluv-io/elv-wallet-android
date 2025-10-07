package app.eluvio.wallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.util.Consumer
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import app.eluvio.wallet.navigation.ComposeNavigator
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.theme.EluvioTheme
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EluvioTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
//                        .background(Color(0xFF050505))
                ) {
                    Image(
                        painterResource(id = R.drawable.bg_gradient),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                    val navController = rememberNavController()
                    DisposableEffect(navController) {
                        val consumer = Consumer<Intent> {
                            Log.d("New intent captured and forwarded to navController: $it")
                            navController.handleDeepLink(it)
                        }
                        this@MainActivity.addOnNewIntentListener(consumer)
                        onDispose {
                            this@MainActivity.removeOnNewIntentListener(consumer)
                        }
                    }
                    val navigator = remember {
                        ComposeNavigator(
                            navController,
                            onBackPressedDispatcherOwner = this@MainActivity
                        )
                    }
                    CompositionLocalProvider(
                        LocalNavigator provides navigator
                    ) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (BuildConfig.DEBUG) {
                            // Print nav backstack for debugging
                            // noinspection RestrictedApi
                            navController.currentBackStack.collectAsState().value.print()
                        }
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

private fun Collection<NavBackStackEntry>.print(prefix: String = "navstack") {
    fun NavBackStackEntry.routeWithArgs(): String {
        val fallback = destination.route ?: ""
        return arguments?.keySet()?.fold(fallback) { route, key ->
            @Suppress("DEPRECATION")
            val value = arguments?.get(key)?.toString() ?: "{$key}"
            route.replace("{$key}", value)
        } ?: fallback
    }

    val stack = map { it.routeWithArgs() }.toTypedArray().contentToString()
    Log.v("$prefix = $stack")
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
                            "app.eluvio.wallet.debug",
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
