package app.eluvio.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

/**
 * Holds one [NavBackStack] per bottom-tab top-level route plus a pointer to the currently
 * selected tab. Matches the
 * [Nav 3 migration guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide#step-3)
 * for apps with multiple back stacks where each tab preserves its own navigation history when
 * the user switches away and back.
 *
 * On Back: pop the active tab's stack. When the stack is back to just its root and the active
 * tab isn't [startRoute], switch to [startRoute]. When the active tab IS [startRoute] and its
 * stack is at root, [stacksInUse] is just `[startRoute]` and `NavDisplay`'s default `onBack`
 * lets the activity finish.
 */
class MobileNavigationState(
    val startRoute: NavKey,
    topLevelRoute: androidx.compose.runtime.MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * Stacks the [androidx.navigation3.ui.NavDisplay] should render right now. When on the start
     * route, just its stack. When on another tab, the start route's stack appears underneath so
     * Back from a deep screen pops into the start tab.
     */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) listOf(startRoute)
        else listOf(startRoute, topLevelRoute)
}

@Composable
fun rememberMobileNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): MobileNavigationState {
    val topLevelRoute = remember { mutableStateOf(startRoute) }
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    return remember(startRoute, topLevelRoutes) {
        MobileNavigationState(startRoute, topLevelRoute, backStacks)
    }
}

/**
 * Flattens [MobileNavigationState.stacksInUse] into the single list of [NavEntry]s that
 * `NavDisplay` consumes. Each per-tab stack is decorated independently so its entries' VMs +
 * saveable state survive switching tabs.
 */
@Composable
fun MobileNavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val decoratedByTab = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider,
        )
    }
    return stacksInUse.flatMap { decoratedByTab[it].orEmpty() }
}
