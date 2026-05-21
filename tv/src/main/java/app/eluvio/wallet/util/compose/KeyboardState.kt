package app.eluvio.wallet.util.compose

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


@Composable
fun keyboardAsState(): State<Boolean> {
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }

    DisposableEffect(LocalWindowInfo.current) {
        val listener = ViewTreeObserver.OnPreDrawListener {
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun KeyboardClosedHandler(onClosed: () -> Unit) {
    val currentOnClose by rememberUpdatedState(onClosed)

    val keyboardOpen by keyboardAsState()
    // Keeps track if the keyboard was open before, so we don't trigger onClose on composition.
    var wasOpen by remember { mutableStateOf(false) }

    LaunchedEffect(keyboardOpen) {
        if (keyboardOpen) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            currentOnClose()
        }
    }
}
