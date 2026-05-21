package app.eluvio.wallet.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme

fun EluvioColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = EluvioColors.Primary,
        surface = EluvioColors.Surface,
        onSurface = EluvioColors.OnSurface,
        inverseSurface = EluvioColors.InverseSurface,
        inverseOnSurface = EluvioColors.InverseOnSurface,
        surfaceVariant = EluvioColors.SurfaceVariant,
        onSurfaceVariant = EluvioColors.OnSurfaceVariant,
        secondaryContainer = EluvioColors.SecondaryContainer,
        onSecondaryContainer = EluvioColors.OnSecondaryContainer,
        border = EluvioColors.Border,
    )
}

val ColorScheme.redeemTagSurface: Color get() = Color(0xFFFFD541)
val ColorScheme.onRedeemTagSurface: Color get() = Color.Black
val ColorScheme.redeemAvailableText: Color get() = redeemTagSurface
val ColorScheme.redeemExpiredText: Color get() = Color(0xFFF34242)

val ColorScheme.disabledItemAlpha: Float get() = 0.5f
