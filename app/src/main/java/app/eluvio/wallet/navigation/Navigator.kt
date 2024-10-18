package app.eluvio.wallet.navigation

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import app.eluvio.wallet.screens.NavGraphs
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.navigation.popUpTo

typealias Navigator = (event: NavigationEvent) -> Unit

/**
 * Convenience method for onClick handlers.
 */
fun Navigator.callbackFor(event: NavigationEvent): () -> Unit = { this(event) }

/**
 * Convenience methods for onClick handlers.
 */
@Composable
fun ProvidableCompositionLocal<Navigator>.callbackFor(event: NavigationEvent): () -> Unit =
    this.current.callbackFor(event)

val LocalNavigator =
    staticCompositionLocalOf<Navigator> { error("No NavigationHandler provided") }

class ComposeNavigator(
    private val navController: NavController,
    private val onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner,
) : Navigator {
    override fun invoke(event: NavigationEvent) {
        when (event) {
            NavigationEvent.GoBack -> {
                // TODO: figure out why I decided to use onBackPressedDispatcherOwner here instead
                //  of navController.popBackStack(). I'm sure there was a reason..
                onBackPressedDispatcherOwner.onBackPressedDispatcher.onBackPressed()
            }

            is NavigationEvent.Push -> {
                navController.navigate(event.direction)
            }

            is NavigationEvent.Replace -> {
                navController.navigate(event.direction) {
                    navController.currentDestination?.route?.let { currentRoute ->
                        popUpTo(currentRoute) {
                            inclusive = true
                        }
                    }
                }
            }

            is NavigationEvent.SetRoot -> {
                navController.navigate(event.direction) { popUpTo(NavGraphs.root) }
            }

            is NavigationEvent.PopTo -> {
                navController.popBackStack(event.route, event.inclusive)
            }
        }
    }
}
