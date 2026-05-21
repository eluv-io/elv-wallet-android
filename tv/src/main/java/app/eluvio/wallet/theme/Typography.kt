package app.eluvio.wallet.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography

@Composable
fun EluvioTypography(): Typography {
    return with(MaterialTheme.typography) {
        copy(
            displayLarge = displayLarge.copy(fontFamily = InterFontFamily),
            displayMedium = displayMedium.copy(fontFamily = InterFontFamily),
            displaySmall = displaySmall.copy(fontFamily = InterFontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = InterFontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = InterFontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = InterFontFamily),
            titleLarge = titleLarge.copy(fontFamily = InterFontFamily),
            titleMedium = titleMedium.copy(fontFamily = InterFontFamily),
            titleSmall = titleSmall.copy(fontFamily = InterFontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = InterFontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = InterFontFamily),
            bodySmall = bodySmall.copy(fontFamily = InterFontFamily),
            labelLarge = labelLarge.copy(fontFamily = InterFontFamily),
            labelMedium = labelMedium.copy(fontFamily = InterFontFamily),
            labelSmall = labelSmall.copy(fontFamily = InterFontFamily),
        )
    }
}

// Re-export shared figma-named styles as Typography extensions so existing call sites
// (MaterialTheme.typography.carousel_48 etc.) keep working.
val Typography.title_62: TextStyle get() = EluvioTextStyles.title_62
val Typography.body_32: TextStyle get() = EluvioTextStyles.body_32
val Typography.carousel_48: TextStyle get() = EluvioTextStyles.carousel_48
val Typography.carousel_36: TextStyle get() = EluvioTextStyles.carousel_36
val Typography.header_53: TextStyle get() = EluvioTextStyles.header_53
val Typography.header_30: TextStyle get() = EluvioTextStyles.header_30
val Typography.button_28: TextStyle get() = EluvioTextStyles.button_28
val Typography.button_24: TextStyle get() = EluvioTextStyles.button_24
val Typography.label_40: TextStyle get() = EluvioTextStyles.label_40
val Typography.label_37: TextStyle get() = EluvioTextStyles.label_37
val Typography.label_24: TextStyle get() = EluvioTextStyles.label_24

/** Dummy object to give access to TextStyle outside of a Compose context. */
val DefaultTypography = Typography()
