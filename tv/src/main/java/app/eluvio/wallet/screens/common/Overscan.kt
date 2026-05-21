package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Recommended padding values for TV apps to account for overscan.
 * See https://developer.android.com/training/tv/start/layouts.html#overscan
 */
object Overscan {

    val horizontalPadding = 48.dp
    val verticalPadding = 27.dp

    fun defaultPadding(
        excludeTop: Boolean = false, excludeBottom: Boolean = false,
        excludeStart: Boolean = false, excludeEnd: Boolean = false
    ): PaddingValues {
        return PaddingValues(
            top = if (excludeTop) 0.dp else verticalPadding,
            bottom = if (excludeBottom) 0.dp else verticalPadding,
            start = if (excludeStart) 0.dp else horizontalPadding,
            end = if (excludeEnd) 0.dp else horizontalPadding
        )
    }

    fun defaultPadding(
        excludeHorizontal: Boolean = false, excludeVertical: Boolean = false,
    ) = defaultPadding(
        excludeTop = excludeVertical, excludeBottom = excludeVertical,
        excludeStart = excludeHorizontal, excludeEnd = excludeHorizontal
    )
}

