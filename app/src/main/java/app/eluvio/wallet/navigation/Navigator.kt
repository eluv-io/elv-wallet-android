package app.eluvio.wallet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

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
