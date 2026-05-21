package app.eluvio.wallet.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.LocalContentColor

@Composable
fun TextStyle.withAlpha(alpha: Float) =
    copy(color = color.takeOrElse { LocalContentColor.current }.copy(alpha = alpha))

@Composable
fun TextStyle.updateColor(block: @Composable Color.() -> Color): TextStyle {
    return copy(color = color.takeOrElse { LocalContentColor.current }.block())
}
