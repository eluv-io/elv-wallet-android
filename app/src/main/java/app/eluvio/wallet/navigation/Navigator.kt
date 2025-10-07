package app.eluvio.wallet.navigation

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.route
import com.ramcosta.composedestinations.utils.toDestinationsNavigator

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
    private val destNavigator = navController.toDestinationsNavigator()
    override fun invoke(event: NavigationEvent) {
        when (event) {
            NavigationEvent.GoBack -> {
                // TODO: figure out why I decided to use onBackPressedDispatcherOwner here instead
                //  of navController.popBackStack(). I'm sure there was a reason..
                onBackPressedDispatcherOwner.onBackPressedDispatcher.onBackPressed()
            }

            is NavigationEvent.Push -> {
                destNavigator.navigate(event.direction)
            }

            is NavigationEvent.Replace -> {
                destNavigator.navigate(event.direction) {
                    navController.currentBackStackEntry?.route()?.let { currentRoute ->
                        popUpTo(currentRoute) {
                            inclusive = true
                        }
                    }
                }
            }

            is NavigationEvent.SetRoot -> {
                destNavigator.navigate(event.direction) { popUpTo(NavGraphs.root) }
            }

            is NavigationEvent.PopTo -> {
                destNavigator.popBackStack(event.route, event.inclusive)
            }
        }
    }
}
