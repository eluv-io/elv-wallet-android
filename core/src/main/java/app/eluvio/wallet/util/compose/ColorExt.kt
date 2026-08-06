package app.eluvio.wallet.util.compose

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun Color.Companion.fromHex(colorString: String) =
    Color(colorString.toColorInt())

/**
 * Convenience function to create black with alpha
 */
@Suppress("FunctionName")
fun Color.Companion.Black(alpha: Float) = Black.copy(alpha = alpha)
