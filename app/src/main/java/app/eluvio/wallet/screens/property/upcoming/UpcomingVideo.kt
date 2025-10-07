package app.eluvio.wallet.screens.property.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Destination<MainGraph>(navArgs = UpcomingVideoNavArgs::class)
@Composable
fun UpcomingVideo() {
    hiltViewModel<UpcomingVideoViewModel>().subscribeToState { vm, state ->
        UpcomingVideo(state)
    }
}

@Composable
private fun UpcomingVideo(state: UpcomingVideoViewModel.State) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (state.imagesBaseUrl == null) {
            // UpcomingVideo is pretty useless without any images, so make sure to only show it
            // once we have a baseUrl to use.
            EluvioLoadingSpinner()
        } else {
            UpcomingVideoContent(state)
        }
    }
}

@Composable
private fun UpcomingVideoContent(state: UpcomingVideoViewModel.State) {
    AsyncImage(
        model = state.backgroundImageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row {
            state.icons.forEach { icon ->
                key(icon) {
                    AsyncImage(
                        model = "${state.imagesBaseUrl}$icon",
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
        if (state.headers.isNotEmpty()) {
            Text(
                text = state.headers.joinToString(separator = "   "),
                style = MaterialTheme.typography.body_32.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFA5A6A8),
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .padding(vertical = 8.dp, horizontal = 22.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        Text(
            text = state.title,
            style = MaterialTheme.typography.body_32.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(26.dp))
        val navigator = LocalNavigator.current
        Countdown(state, onComplete = {
            // Countdown done. Navigate to the video player.
            // Disabled - vm will take care of this.
//            navigator(
//                VideoPlayerActivityDestination(
//                    propertyId = state.propertyId,
//                    mediaItemId = state.mediaItemId
//                ).asReplace()
//            )
        })
    }
}

@Composable
private fun Countdown(
    state: UpcomingVideoViewModel.State,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit,
) {
    var remainingTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(state.startTimeMillis) {
        while (true) {
            val timeRemaining = state.remainingTimeToStart ?: break
            if (timeRemaining.second <= 0) {
                onComplete()
                break
            }
            remainingTimeStr = timeRemaining.first

            // Update every second
            delay(1.seconds)
        }
    }
    Text(
        text = remainingTimeStr,
        style = MaterialTheme.typography.title_62.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun UpcomingVideoPreview() = EluvioThemePreview {
    UpcomingVideo(
        UpcomingVideoViewModel.State(
            mediaItemId = "123",
            title = "Upcoming Video",
            headers = listOf("Header 1", "Header 2"),
            icons = listOf("icon1", "icon2"),
            startTimeMillis = System.currentTimeMillis() / 1000 + 70,
            imagesBaseUrl = ""
        )
    )
}
