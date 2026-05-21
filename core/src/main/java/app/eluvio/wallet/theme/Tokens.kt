package app.eluvio.wallet.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.eluvio.wallet.core.R

/**
 * Toolkit-agnostic design tokens used by both `:tv` (androidx.tv.material3) and
 * `:mobile` (androidx.compose.material3). Each app wires these into its own
 * `MaterialTheme` since the TV and mobile Material 3 libs have separate type
 * hierarchies for ColorScheme / Typography wrappers.
 */

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_bold, weight = FontWeight.Bold),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_thin, weight = FontWeight.Thin),
)

/** Shared color palette. Each app passes these into its toolkit's `darkColorScheme(...)`. */
object EluvioColors {
    val Primary = Color.White
    val Surface = Color(0xFF3E3F40)
    val OnSurface = Color.White
    val InverseSurface = Color(0xFFD4D4D4)
    val InverseOnSurface = Color.Black
    val SurfaceVariant = Color.White
    val OnSurfaceVariant = Color.Black
    val SecondaryContainer = Color(0xFF626262)
    val OnSecondaryContainer = Color.White
    val Border = Color.White
}

/**
 * Shared custom text styles (Figma-named). Use directly, or re-export as extensions on
 * each toolkit's `Typography` if you prefer the `MaterialTheme.typography.foo` style.
 */
object EluvioTextStyles {
    val title_62 = eluvioTextStyle(31.sp, FontWeight.SemiBold)
    val body_32 = eluvioTextStyle(16.sp, FontWeight.Normal)
    val carousel_48 = eluvioTextStyle(24.sp, FontWeight.Normal)
    val carousel_36 = eluvioTextStyle(18.sp, FontWeight.Normal)
    val header_53 = eluvioTextStyle(26.sp, FontWeight.Normal)
    val header_30 = eluvioTextStyle(15.sp, FontWeight.Normal)
    val button_28 = eluvioTextStyle(14.sp, FontWeight.SemiBold)
    val button_24 = eluvioTextStyle(12.sp, FontWeight.SemiBold)
    val label_40 = eluvioTextStyle(20.sp, FontWeight.Medium)
    val label_37 = eluvioTextStyle(18.sp, FontWeight.Bold)
    val label_24 = eluvioTextStyle(12.sp, FontWeight.Medium)
}

private fun eluvioTextStyle(size: TextUnit, fontWeight: FontWeight) = TextStyle(
    fontSize = size,
    fontFamily = InterFontFamily,
    fontWeight = fontWeight,
)
