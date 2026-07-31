package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.R
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.util.compose.requestOnce
import coil3.compose.AsyncImage

@Composable
fun SinglePropertyPage(
    state: DiscoverViewModel.State,
    @Suppress("UNUSED_PARAMETER") onBackgroundImageSet: (FabricUrl?) -> Unit,
    onPropertyClicked: (DiscoverViewModel.State.Property) -> Unit,
    onRetryClicked: () -> Unit
) {
    val property = state.properties.firstOrNull()
    // The background is drawn here rather than handed to Dashboard via onBackgroundImageSet
    // (which stays on the signature for parity with DiscoverGrid, but goes deliberately
    // unused): this page assumes it owns the full screen, so there's no side nav rail or other
    // element outside this composable's bounds to account for. Drawing it here also lets the
    // background reuse the same baked-in placeholder/fallback treatment as the logo below.
    val bakedBackground = painterResource(R.drawable.start_screen_background)
    AsyncImage(
        property?.startScreenBackground,
        contentDescription = null,
        placeholder = bakedBackground,
        error = bakedBackground,
        fallback = bakedBackground,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    if (state.loading) {
        EluvioLoadingSpinner(Modifier.padding(top = 100.dp))
    } else if (property != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // Baked-in art covers all three gaps: while the Property's own logo is still
            // downloading, if that download fails, and if Content Studio has no
            // start_screen_logo at all (fallback). Whitelabel builds override the drawable.
            val bakedLogo = painterResource(R.drawable.start_screen_logo)
            AsyncImage(
                property.startScreenLogo,
                contentDescription = "${property.name} Logo",
                placeholder = bakedLogo,
                error = bakedLogo,
                fallback = bakedLogo,
                modifier = Modifier.height(160.dp)
            )
            // Not requestInitialFocus(): its "only once per configuration" guard survives a trip
            // to Sign In and back, and this screen has no other focusable to fall back on, so
            // the button would come back unfocused. It's the only target here — always take focus.
            val focusRequester = remember { FocusRequester() }
            TvButton(
                if (state.isLoggedIn) "Welcome Back" else "Sign In",
                onClick = { onPropertyClicked(property) },
                Modifier.focusRequester(focusRequester)
            )
            focusRequester.requestOnce()
        }
    } else if (state.showRetryButton) {
        RetryButton(onRetryClicked, Modifier.padding(top = 100.dp))
    }
}
