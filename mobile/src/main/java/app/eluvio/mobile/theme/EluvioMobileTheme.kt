package app.eluvio.mobile.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.eluvio.wallet.theme.EluvioColors
import app.eluvio.wallet.theme.InterFontFamily

/**
 * Mobile equivalent of `:tv`'s `EluvioTheme`. Wraps `androidx.compose.material3.MaterialTheme`
 * with shared color tokens + Inter font from `:core/theme/Tokens.kt`. Mirrors the XML
 * `Theme.Material3.Dark.NoActionBar` parent applied at the Activity level — Compose doesn't
 * inherit from the host Activity's XML theme, so we wire colours explicitly here.
 */
@Composable
fun EluvioMobileTheme(content: @Composable () -> Unit) {
    // Use M3's default dark scheme for surface/onSurface/onSurfaceVariant/etc — those
    // slots are tuned for the Material 3 component behavior on mobile (e.g. unselected
    // NavigationBarItem reads onSurfaceVariant, secondary text reads onSurfaceVariant).
    // Only override the brand colour. TV's `EluvioColors` palette intentionally remaps
    // surfaceVariant to white, which is fine for the TV design but breaks mobile components.
    val colorScheme = remember {
        darkColorScheme(primary = EluvioColors.Primary)
    }
    val typography = remember { defaultTypographyWithInter() }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}

private fun defaultTypographyWithInter(): Typography {
    val default = Typography()
    return default.copy(
        displayLarge = default.displayLarge.copy(fontFamily = InterFontFamily),
        displayMedium = default.displayMedium.copy(fontFamily = InterFontFamily),
        displaySmall = default.displaySmall.copy(fontFamily = InterFontFamily),
        headlineLarge = default.headlineLarge.copy(fontFamily = InterFontFamily),
        headlineMedium = default.headlineMedium.copy(fontFamily = InterFontFamily),
        headlineSmall = default.headlineSmall.copy(fontFamily = InterFontFamily),
        titleLarge = default.titleLarge.copy(fontFamily = InterFontFamily),
        titleMedium = default.titleMedium.copy(fontFamily = InterFontFamily),
        titleSmall = default.titleSmall.copy(fontFamily = InterFontFamily),
        bodyLarge = default.bodyLarge.copy(fontFamily = InterFontFamily),
        bodyMedium = default.bodyMedium.copy(fontFamily = InterFontFamily),
        bodySmall = default.bodySmall.copy(fontFamily = InterFontFamily),
        labelLarge = default.labelLarge.copy(fontFamily = InterFontFamily),
        labelMedium = default.labelMedium.copy(fontFamily = InterFontFamily),
        labelSmall = default.labelSmall.copy(fontFamily = InterFontFamily),
    )
}
