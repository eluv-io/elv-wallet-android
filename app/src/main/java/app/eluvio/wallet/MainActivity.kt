package app.eluvio.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.util.Consumer
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import app.eluvio.wallet.navigation.ComposeNavigator
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.screens.NavGraphs
import app.eluvio.wallet.theme.EluvioTheme
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EluvioTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF050505))
                ) {
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
                        LocalNavigator provides navigator,
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface
                    ) {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                        )
                        // Print nav backstack for debugging
                        navController.currentBackStack.collectAsState().value.print()
                    }
                }
            }
        }
    }
}

//private val backgroundBrush: ShaderBrush = object : ShaderBrush() {
//    override fun createShader(size: Size): Shader {
//        return RadialGradientShader(
//            colors = listOf(Color(0xff202020), Color.Black),
//            center = size.toRect().topCenter,
//            radius = size.width / 4f,
//            colorStops = listOf(0f, 0.95f)
//        )
//    }
//}

private fun Collection<NavBackStackEntry>.print(prefix: String = "stack") {
    fun NavBackStackEntry.routeWithArgs(): String {
        val fallback = destination.route ?: ""
        return arguments?.keySet()?.fold(fallback) { route, key ->
            route.replace("{$key}", arguments?.get(key)?.toString() ?: "{$key}")
        } ?: fallback
    }

    val stack = map { it.routeWithArgs() }.toTypedArray().contentToString()
    Log.v("$prefix = $stack")
}
