package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.carousel_48

@Composable
fun SectionHeader(
    item: DynamicPageLayoutState.Section.SectionHeader,
    modifier: Modifier = Modifier,
) {
    Text(
        item.text,
        style = MaterialTheme.typography.carousel_48.copy(fontSize = 22.sp),
        modifier = modifier.padding(
            start = Overscan.horizontalPadding,
            top = 0.dp,
            end = 80.dp,
            bottom = 0.dp
        )
    )
}
