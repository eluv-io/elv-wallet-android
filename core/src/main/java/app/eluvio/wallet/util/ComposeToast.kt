package app.eluvio.wallet.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberToaster(): Toaster {
    val context = LocalContext.current
    return remember { Toaster(context) }
}
