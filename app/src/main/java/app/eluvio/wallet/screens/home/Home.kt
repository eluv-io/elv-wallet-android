package app.eluvio.wallet.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.parameters.DeepLink

// TODO: home is a bad name, this is just the log that decides where to go when app is opened
@Destination<MainGraph>(
    start = true,
    navArgs = DeeplinkArgs::class,
    // Handles direct deep links
    deepLinks = [
        DeepLink(
            uriPattern = "elvwallet://{action}/{marketplace}/{contract}/{sku}?jwt={jwt}&entitlement={entitlement}&back_link={backLink}",
        ),
    ]
)
@Composable
fun Home() {
    hiltViewModel<HomeViewModel>().subscribeToState { _, state ->
        if (state.showLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                EluvioLoadingSpinner()
            }
        }
    }
}
