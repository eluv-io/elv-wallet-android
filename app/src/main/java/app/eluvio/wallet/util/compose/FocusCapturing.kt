package app.eluvio.wallet.util.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer


/**
 * Compose focus handing HEAVILY prefers moving focus in the direction of the scroll. This can be a
 * problem when you have a row where the focusable elements are offset a few pixels to the side, and
 * are not directly below/above the last element. This modifier works around that by "fooling" the
 * compose focus system into treating the entire row as a single focusable element, then delegates
 * the focus to [focusRestorer].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusCapturingLazyList(
    listState: LazyListState,
    childFocusRequesters: List<FocusRequester>
) =this
//    focusProperties {
//        enter = {
//            // Doesn't actually matter that it doesn't match the "correct" index, because
//            // "focusRestorer" will take care of it down the line. All we need to make sure is to
//            // return a FocusRequester that isn't Default, and is attached to the screen.
//            childFocusRequesters[listState.firstVisibleItemIndex]
//        }
//    }
//        .focusGroup() // Required to make .focusable() work
//        .focusable() // Actually captures focus so we get the enter callback
//        .focusGroup() // Required to make .focusRestorer() work
//        .focusRestorer() // Handles restoring focus to the last focused item.

/**
 * NOTE: Do NOT use this for Lazy Lists. Use [focusCapturingLazyList] instead.
 * NOTE2: Make sure you are providing a FocusRequester that is actually attached.
 *   During fast scrolling, this could be invoked on the parent before children are positioned.
 *
 * Compose focus handing HEAVILY prefers moving focus in the direction of the scroll. This can be a
 * problem when you have a row where the focusable elements are offset a few pixels to the side, and
 * are not directly below/above the last element. This modifier works around that by "fooling" the
 * compose focus system into treating the entire row as a single focusable element, then delegates
 * the focus to [focusRestorer].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusCapturingGroup(someChildFocusRequester: () -> FocusRequester) =
    this
//    focusProperties {
//        enter = {
//            // Doesn't actually matter that it doesn't match the "correct" index, because
//            // "focusRestorer" will take care of it down the line. All we need to make sure is to
//            // return a FocusRequester that isn't Default, and is attached to the screen.
//            someChildFocusRequester()
//        }
//    }
//        .focusGroup() // Required to make .focusable() work
//        .focusable() // Actually captures focus so we get the enter callback
//        .focusGroup() // Required to make .focusRestorer() work
//        .focusRestorer() // Handles restoring focus to the last focused item.
//        // I don't know why this last .focusGroup() is needed in the non-lazy version,
//        // yet it breaks the lazy version, but here we are :shrug:.
//        .focusGroup()
