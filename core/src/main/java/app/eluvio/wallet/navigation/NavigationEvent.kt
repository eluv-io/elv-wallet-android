package app.eluvio.wallet.navigation

import androidx.compose.runtime.Immutable

@Immutable
sealed interface NavigationEvent {
    data object GoBack : NavigationEvent
    data class SetRoot(val target: NavTarget) : NavigationEvent
    data class Push(val target: NavTarget) : NavigationEvent
    data class Replace(val target: NavTarget) : NavigationEvent
}

fun NavTarget.asPush() = NavigationEvent.Push(this)

fun NavTarget.asReplace() = NavigationEvent.Replace(this)

fun NavTarget.asNewRoot() = NavigationEvent.SetRoot(this)
