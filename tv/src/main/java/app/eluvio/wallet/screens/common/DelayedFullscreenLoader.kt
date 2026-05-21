package app.eluvio.wallet.screens.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DelayedFullscreenLoader(
    modifier: Modifier = Modifier,
    delay: Duration = 400.milliseconds,
    content: @Composable () -> Unit = { EluvioLoadingSpinner() }
) {
    var actuallyShowLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Prevent flicker by showing loading spinner only after a delay
        delay(delay)
        actuallyShowLoading = true
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().then(modifier)
    ) {
        if (actuallyShowLoading) {
            content()
        }
    }
}
