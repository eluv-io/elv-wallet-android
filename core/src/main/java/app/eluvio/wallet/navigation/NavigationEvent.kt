package app.eluvio.wallet.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey

@Immutable
sealed interface NavigationEvent {
    data object GoBack : NavigationEvent
    data class SetRoot(val target: NavKey) : NavigationEvent
    data class Push(val target: NavKey) : NavigationEvent
    data class Replace(val target: NavKey) : NavigationEvent
}

fun NavKey.asPush() = NavigationEvent.Push(this)

fun NavKey.asReplace() = NavigationEvent.Replace(this)

fun NavKey.asNewRoot() = NavigationEvent.SetRoot(this)
