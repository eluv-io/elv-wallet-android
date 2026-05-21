package app.eluvio.wallet.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import app.eluvio.wallet.R


@Composable
fun EluvioTypography(): Typography {
    return with(MaterialTheme.typography) {
        copy(
            displayLarge = displayLarge.copy(fontFamily = interFontFamily),
            displayMedium = displayMedium.copy(fontFamily = interFontFamily),
            displaySmall = displaySmall.copy(fontFamily = interFontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = interFontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = interFontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = interFontFamily),
            titleLarge = titleLarge.copy(fontFamily = interFontFamily),
            titleMedium = titleMedium.copy(fontFamily = interFontFamily),
            titleSmall = titleSmall.copy(fontFamily = interFontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = interFontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = interFontFamily),
            bodySmall = bodySmall.copy(fontFamily = interFontFamily),
            labelLarge = labelLarge.copy(fontFamily = interFontFamily),
            labelMedium = labelMedium.copy(fontFamily = interFontFamily),
            labelSmall = labelSmall.copy(fontFamily = interFontFamily),
        )
    }
}

// Custom typography types, matching figma names
val Typography.title_62: TextStyle
    get() = eluvioTextStyle(
        size = 31.sp,
        fontWeight = FontWeight.SemiBold,
    )
val Typography.body_32: TextStyle
    get() = eluvioTextStyle(
        size = 16.sp,
        fontWeight = FontWeight.Normal,
    )

val Typography.carousel_48: TextStyle
    get() = eluvioTextStyle(
        size = 24.sp,
        fontWeight = FontWeight.Normal,
    )
val Typography.carousel_36: TextStyle
    get() = eluvioTextStyle(
        size = 18.sp,
        fontWeight = FontWeight.Normal,
    )

val Typography.header_53: TextStyle
    get() = eluvioTextStyle(
        size = 26.sp,
        fontWeight = FontWeight.Normal,
    )
val Typography.header_30: TextStyle
    get() = eluvioTextStyle(
        size = 15.sp,
        fontWeight = FontWeight.Normal,
    )

val Typography.button_28: TextStyle
    get() = eluvioTextStyle(
        size = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
val Typography.button_24: TextStyle
    get() = eluvioTextStyle(
        size = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )

val Typography.label_40: TextStyle
    get() = eluvioTextStyle(
        size = 20.sp,
        fontWeight = FontWeight.Medium,
    )
val Typography.label_37: TextStyle
    get() = eluvioTextStyle(
        size = 18.sp,
        fontWeight = FontWeight.Bold,
    )
val Typography.label_24: TextStyle
    get() = eluvioTextStyle(
        size = 12.sp,
        fontWeight = FontWeight.Medium,
    )

/**
 * Dummy object to give access to TextStyle outside of a Compose context.
 */
val DefaultTypography = Typography()

private fun eluvioTextStyle(size: TextUnit, fontWeight: FontWeight): TextStyle {
    return TextStyle(
        fontSize = size,
        fontFamily = interFontFamily,
        fontWeight = fontWeight,
    )
}

private val interFontFamily =
    FontFamily(
        Font(R.font.inter_regular, weight = FontWeight.Normal),
        Font(R.font.inter_medium, weight = FontWeight.Medium),
        Font(R.font.inter_bold, weight = FontWeight.Bold),
        Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
        Font(R.font.inter_thin, weight = FontWeight.Thin),
    )
