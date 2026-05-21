package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.theme.label_40


@Composable
fun <Value> SearchFilterChip(
    title: String,
    value: Value,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Value) -> Unit,
    onFocus: (Value) -> Unit = onClick,
) {
    Surface(
        onClick = { onClick(value) },
        modifier = modifier.onFocusChanged {
            if (it.hasFocus) {
                onFocus(value)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    top = 5.dp,
                    bottom = 5.dp,
                    start = 20.dp,
                    end = if (selected) 8.dp else 20.dp
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.label_40,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Clear",
                    Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}
