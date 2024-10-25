package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.theme.label_24

@Composable
fun DescriptionSection(item: DynamicPageLayoutState.Section.Description, modifier: Modifier = Modifier) {
    var isClickable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    Text(
        text = item.text,
        style = MaterialTheme.typography.label_24,
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
            // Only clickable if there's actually overflow.
            isClickable = expanded || textLayoutResult.hasVisualOverflow
        },
        modifier = modifier
            .clickable(enabled = isClickable, onClick = { expanded = !expanded })
            .padding(
                start = Overscan.horizontalPadding,
                end = 380.dp,
                bottom = 36.dp
            )
    )
}
