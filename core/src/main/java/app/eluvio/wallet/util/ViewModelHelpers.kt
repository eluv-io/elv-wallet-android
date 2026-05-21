package app.eluvio.wallet.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.util.logging.Log

/**
 * @param [onEvent] return true if event was handled, false otherwise.
 */
@Composable
inline fun <reified VM : BaseViewModel<State>, State : Any> VM.subscribeToState(
    crossinline onEvent: (Events) -> Boolean = { false },
    onState: @Composable (VM, State) -> Unit
) {
    val vm = this
    val navigator = LocalNavigator.current
    val toaster = rememberToaster()
    // Handle navigation events
    DisposableEffect(Unit) {
        val navigationEvents = vm.navigationEvents.subscribe {
            Log.d("${vm.javaClass.simpleName} navigating to $it")
            navigator(it)
        }
        val events = vm.events.subscribe {
            if (onEvent(it)) {
                Log.d("${vm.javaClass.simpleName} handled event: $it")
            } else {
                Log.d("${vm.javaClass.simpleName} default event handling: $it")
                defaultEventHandler(it, toaster)
            }
        }
        onDispose {
            navigationEvents.dispose()
            events.dispose()
        }
    }

    vm.state.subscribeAsState(initial = null).value?.let { state ->
        onState(vm, state)
    }

    // Important to register AFTER we subscribed to state/nav/events, otherwise we might miss events emitted by VM
    vm.observeLifecycle(LocalLifecycleOwner.current.lifecycle)
}

fun defaultEventHandler(event: Events, toaster: Toaster) {
    when (event) {
        is Events.ToastMessage -> toaster.toast(event.message)
    }
}

@Composable
fun BaseViewModel<*>.observeLifecycle(lifecycle: Lifecycle) {
    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = onResume()
            override fun onPause(owner: LifecycleOwner) = onPause()
        }
        lifecycle.addObserver(observer)
        onDispose {
            // The composition can be disposed without the lifecycle going thru onPause.
            // This has the potential of calling onPause twice in a row, but the ViewModel will
            // handle it.
            onPause()
            lifecycle.removeObserver(observer)
        }
    }
}
