package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.foundation.lazy.list.TvLazyListScope

/**
 * Convenience method to add a [Spacer] to a [TvLazyListScope].
 * Using [contentPadding] will clip items so they won't draw over the padding when scrolled.
 * This solves that by just adding an un-focusable item that takes up space.
 */
fun TvLazyListScope.spacer(
    height: Dp = Dp.Unspecified,
    width: Dp = Dp.Unspecified,
    key: Any? = null
) {
    item(key = key, contentType = "list_spacer") {
        Spacer(Modifier.size(width = width, height = height))
    }
}

fun LazyListScope.spacer(
    height: Dp = Dp.Unspecified,
    width: Dp = Dp.Unspecified,
    key: Any? = null
) {
    item(key = key, contentType = "list_spacer") {
        Spacer(Modifier.size(width = width, height = height))
    }
}
