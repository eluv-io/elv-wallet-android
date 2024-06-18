package app.eluvio.wallet.util.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer

/**
 * A modifier that will apply the given [block] if the given [condition] is true.
 */
fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier {
    return if (condition) then(Modifier.block()) else this
}

/**
 * A focus restorer that will delegate to the given [FocusRequester] when [onRestoreFailed] is
 * called, but only once per composition/configuration.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusRestorer(oneTimeFallback: FocusRequester): Modifier = composed {
    var ranOnce by rememberSaveable { mutableStateOf(false) }
    focusRestorer {
        if (!ranOnce) {
            ranOnce = true
            oneTimeFallback
        } else {
            FocusRequester.Default
        }
    }
}
