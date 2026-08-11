package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.theme.label_40

@Composable
internal fun RetryButton(onRetryClicked: () -> Unit, modifier: Modifier = Modifier.Companion) {
    Box(contentAlignment = Alignment.Center) {
        TvButton(
            onClick = onRetryClicked,
            modifier = modifier
        ) {
            Row(
                Modifier.padding(
                    top = 5.dp,
                    bottom = 5.dp,
                    start = 20.dp,
                    end = 14.dp
                )
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.label_40,
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear",
                    Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}
