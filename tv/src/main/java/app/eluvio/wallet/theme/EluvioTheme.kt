package app.eluvio.wallet.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import app.eluvio.wallet.navigation.LocalNavigator

@Composable
fun EluvioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EluvioColorScheme(),
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(5.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(5.dp),
            extraLarge = RoundedCornerShape(5.dp),
        ),
        typography = EluvioTypography(),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            content()
        }
    }
}

@Composable
fun EluvioThemePreview(content: @Composable BoxScope.() -> Unit) {
    EluvioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
        ) {
            CompositionLocalProvider(
                LocalNavigator provides {},
                LocalSurfaceScale provides LocalSurfaceScale.current,
            ) {
                content()
            }
        }
    }
}

val LocalSurfaceScale =
    staticCompositionLocalOf { ClickableSurfaceDefaults.scale(focusedScale = 1.05f) }
