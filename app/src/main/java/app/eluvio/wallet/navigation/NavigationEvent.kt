package app.eluvio.wallet.navigation

import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.Route

sealed interface NavigationEvent {
    object GoBack : NavigationEvent
    data class SetRoot(val direction: Direction) : NavigationEvent
    data class Push(val direction: Direction) : NavigationEvent
    data class Replace(val direction: Direction) : NavigationEvent
    data class PopTo(val route: Route, val inclusive: Boolean) : NavigationEvent
}

fun Direction.asPush() =
    NavigationEvent.Push(this)

fun Direction.asReplace() = NavigationEvent.Replace(this)

fun Direction.asNewRoot() = NavigationEvent.SetRoot(this)
