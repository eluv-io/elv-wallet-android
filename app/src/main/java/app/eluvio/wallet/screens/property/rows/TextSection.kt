package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState

@Composable
fun TextSection(item: DynamicPageLayoutState.Section.Text, modifier: Modifier = Modifier) {
    Text(
        item.text,
        style = item.textStyle,
        modifier = modifier.padding(
            start = Overscan.horizontalPadding,
            top = 0.dp,
            end = 80.dp,
            bottom = 0.dp
        )
    )
}
