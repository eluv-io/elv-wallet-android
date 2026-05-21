package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpRect
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.TabRowScope

@Composable
fun TabRowScope.EluvioTab(
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Tab(
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        colors = TabDefaults.pillIndicatorTabColors(
            contentColor = Color(0xFFB8B8B8),
            inactiveContentColor = Color(0xFFB8B8B8),
            selectedContentColor = Color(0xFFBBBBBB),
            focusedContentColor = Color.Black
        ),
        modifier = modifier,
        content = content
    )
}

val EluvioTabIndicator: @Composable (selectedTabIndex: Int, tabPositions: List<DpRect>, doesTabRowHaveFocus: Boolean) -> Unit =
    @Composable { selectedTabIndex, tabPositions, doesTabRowHaveFocus ->
        tabPositions.getOrNull(selectedTabIndex)?.let {
            TabRowDefaults.PillIndicator(
                currentTabPosition = it,
                doesTabRowHaveFocus = doesTabRowHaveFocus,
                activeColor = Color(0xFFD0D0D0),
                inactiveColor = Color(0xFF838485)
            )
        }
    }
