package app.eluvio.mobile.screens.myitems

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.eluvio.mobile.R

// Placeholder — same as the XML it replaces. Real content lands when MyItemsViewModel is wired.
@Composable
fun MyItemsScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.tab_my_items),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
