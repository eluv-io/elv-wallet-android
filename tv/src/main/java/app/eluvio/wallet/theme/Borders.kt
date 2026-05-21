package app.eluvio.wallet.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme

val MaterialTheme.borders: Borders get() = Borders

object Borders

@Composable
fun Borders.focusedBorder(
    stokeWidth: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.border
) = ClickableSurfaceDefaults.border(
    focusedBorder = Border(BorderStroke(stokeWidth, color))
)

val Borders.focusedBorder: ClickableSurfaceBorder
    @Composable
    get() = focusedBorder()
