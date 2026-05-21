package app.eluvio.wallet.util.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import kotlinx.coroutines.delay

/**
 * Requests focus as a LaunchedEffect once.
 */
@SuppressLint("ComposableNaming")
@Composable
inline fun FocusRequester.requestOnce(key: Any? = Unit) {
    LaunchedEffect(key) {
        requestFocus()
    }
}

/**
 * Requests focus once per activity configuration (not per initial composition).
 * If no [focusRequester] is provided, a new one will be created.
 */
fun Modifier.requestInitialFocus(focusRequester: FocusRequester? = null) = composed {
    // Save the state across recompositions AND configuration changes, so we don't steal focus after
    // coming back from a different screen
    var ranOnce by rememberSaveable { mutableStateOf(false) }
    val requester = remember { focusRequester ?: FocusRequester() }
    LaunchedEffect(requester) {
        if (!ranOnce) {
            ranOnce = true
            // Delay 1ms to make sure this happens after composition
            delay(1)
            requester.requestFocus()
        }
    }
    focusRequester(requester)
}

/**
 * A focus restorer that will delegate to the given [FocusRequester] when [onRestoreFailed] is
 * called, but only once per composition/configuration.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusRestorer2(oneTimeFallback: FocusRequester): Modifier = composed {
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

/**
 * Prevent focus from leaving the current focus group in the specified directions.
 *
 * Note: Generic type is only a workaround for using varags with value classes.
 *
 * See: [KT-33565](https://youtrack.jetbrains.com/issue/KT-33565/Allow-vararg-parameter-of-inline-class-type#focus=Comments-27-6413844.0-0)
 */
@OptIn(ExperimentalComposeUiApi::class)
fun <T : FocusDirection> Modifier.focusTrap(vararg directions: T): Modifier =
    focusProperties {
        exit = { direction ->
            if (direction in directions) {
                FocusRequester.Cancel
            } else {
                FocusRequester.Default
            }
        }
    }
