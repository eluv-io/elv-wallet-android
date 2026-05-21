package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.theme.label_24
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun BoxScope.MetadataTexts(displaySettings: DisplaySettings?) = displaySettings?.run {
    Column(
        Modifier
            .align(Alignment.BottomStart)
            .padding(23.dp)
    ) {
        MetadataTexts(headers.toImmutableList(), title, subtitle)
    }
}

@Composable
fun ColumnScope.MetadataTexts(displaySettings: DisplaySettings?) = displaySettings?.run {
    MetadataTexts(headers.toImmutableList(), title, subtitle)
}

@Composable
fun ColumnScope.MetadataTexts(
    headers: ImmutableList<String>,
    title: String?,
    subtitle: String?
) {
    if (headers.isNotEmpty()) {
        Text(
            text = headers.joinToString(separator = "   ", transform = { it.uppercase() }),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.label_24.copy(fontSize = 8.sp),
            color = Color(0xFFA5A6A8),
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
    title?.takeIf { it.isNotEmpty() }?.let { title ->
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.label_24.copy(fontSize = 12.sp),
        )
    }
    subtitle?.takeIf { it.isNotEmpty() }?.let { subtitle ->
        Text(
            text = subtitle.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.label_24.copy(fontSize = 10.sp),
            color = Color(0xFF818590),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
