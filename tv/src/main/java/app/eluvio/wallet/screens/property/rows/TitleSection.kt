package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.carousel_48

@Composable
fun TitleSection(item: DynamicPageLayoutState.Section.Title, modifier: Modifier = Modifier) {
    Text(
        item.text,
        style = MaterialTheme.typography.carousel_48.copy(fontWeight = FontWeight.Bold),
        modifier = modifier.padding(
            start = Overscan.horizontalPadding,
            top = 0.dp,
            end = 380.dp,
            bottom = 20.dp
        )
    )
}
