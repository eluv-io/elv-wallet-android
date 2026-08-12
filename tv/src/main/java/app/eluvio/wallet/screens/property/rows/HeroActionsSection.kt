package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.HeroAction
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.button_28
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HeroActionsSection(
    item: DynamicPageLayoutState.Section.HeroActions,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(
            start = Overscan.horizontalPadding,
            end = Overscan.horizontalPadding,
            bottom = 40.dp
        )
    ) {
        item.actions.forEach { action ->
            HeroActionButton(action, onClick = { navigator(action.navigationEvent) })
        }
    }
}

@Composable
private fun HeroActionButton(action: HeroAction, onClick: () -> Unit) {
    val shape = RoundedCornerShape(action.cornerRadius)
    TvButton(
        text = action.text,
        onClick = onClick,
        textStyle = MaterialTheme.typography.button_28,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = action.backgroundColor,
            contentColor = action.textColor,
            // Server colors are the brand's, so keep them on focus and rely on the
            // scale+border to show focus instead.
            focusedContainerColor = action.backgroundColor,
            focusedContentColor = action.textColor,
            pressedContainerColor = action.backgroundColor,
            pressedContentColor = action.textColor,
        ),
        shape = ClickableSurfaceDefaults.shape(shape),
        border = ClickableSurfaceDefaults.border(
            border = action.borderColor
                ?.let { Border(BorderStroke(2.dp, it), shape = shape) }
                ?: Border.None,
            focusedBorder = Border(
                BorderStroke(2.dp, MaterialTheme.colorScheme.border),
                shape = shape
            ),
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun HeroActionsSectionPreview() = EluvioThemePreview {
    HeroActionsSection(
        DynamicPageLayoutState.Section.HeroActions(
            sectionId = "1",
            actions = persistentListOf(
                HeroAction(
                    id = "1",
                    text = "WATCH NOW",
                    backgroundColor = Color(0xFF75FF61),
                    textColor = Color(0xFF103234),
                    borderColor = null,
                    cornerRadius = 5.dp,
                    navigationEvent = NavigationEvent.GoBack,
                ),
                HeroAction(
                    id = "2",
                    text = "MORE INFO",
                    backgroundColor = Color.White,
                    textColor = Color.Black,
                    borderColor = Color.Black,
                    cornerRadius = 20.dp,
                    navigationEvent = NavigationEvent.GoBack,
                ),
            )
        )
    )
}
