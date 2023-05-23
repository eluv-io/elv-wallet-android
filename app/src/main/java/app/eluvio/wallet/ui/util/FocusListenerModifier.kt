package app.eluvio.wallet.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Only returns callbacks when elements gains focus.
 */
fun Modifier.onFocused(
    onFocused: () -> Unit
): Modifier = onFocusChanged {
    if (it.isFocused) {
        onFocused()
    }
}