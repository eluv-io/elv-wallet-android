package app.eluvio.wallet.screens.deeplink

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.dashboard.myitems.MediaCard
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.requestOnce
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@Destination<MainGraph>(navArgs = NftClaimNavArgs::class)
@Composable
fun NftClaim() {
    hiltViewModel<NftClaimViewModel>().subscribeToState { vm, state ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            NftClaim(state, vm::claimNft)
        }
    }
}

@Composable
private fun NftClaim(state: NftClaimViewModel.State, onClaimClick: () -> Unit) {
    val media = state.media
    if (state.loading || media == null) {
        EluvioLoadingSpinner()
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(0.65f)
    ) {
        MediaCard(media, modifier = Modifier.width(210.dp))
        Spacer(Modifier.width(50.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(text = media.title, style = MaterialTheme.typography.title_62)
            Spacer(modifier = Modifier.height(12.dp))
            //todo: timestamp goes here
            Text(text = media.description, style = MaterialTheme.typography.body_32)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val focusRequester = remember { FocusRequester() }
                val buttonText = if (state.claimingInProgress) {
                    val numberOfDots by animateDots()
                    "Activating${".".repeat(numberOfDots).padEnd(3, ' ')}"
                } else {
                    "Activate Pass"
                }
                TvButton(
                    text = buttonText,
                    onClick = onClaimClick,
                    modifier = Modifier.focusRequester(focusRequester),
                    scale = ClickableSurfaceDefaults.scale(
                        focusedScale = 1.0f,
                        pressedScale = 0.9f
                    ),
                    enabled = !state.claimingInProgress
                )
                focusRequester.requestOnce()
                Spacer(modifier = Modifier.width(8.dp))
                if (state.claimingInProgress) {
                    Text(
                        text = "This make take up to one minute.",
                        style = MaterialTheme.typography.label_24,
                        color = Color(0xFF959595)
                    )
                }
            }
        }
    }
}

@Composable
private fun animateDots(numberOfDots: Int = 3): State<Int> {
    val intToVector: TwoWayConverter<Int, AnimationVector1D> =
        TwoWayConverter({ AnimationVector1D(it.toFloat()) }, { it.value.toInt() })
    return rememberInfiniteTransition(label = "ellipsis transition")
        .animateValue(
            initialValue = 1,
            targetValue = numberOfDots + 1,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing)
            ),
            label = "ellipsis animation",
            typeConverter = intToVector
        )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun NftClaimPreview() = EluvioThemePreview {
    NftClaim(NftClaimViewModel.State(), onClaimClick = {})
}
