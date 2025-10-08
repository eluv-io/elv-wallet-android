package app.eluvio.wallet.screens.property

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

/**
 * See [DynamicPageLayout] for @Preview
 */
@Destination<MainGraph>(navArgs = PropertyDetailNavArgs::class)
@Composable
fun PropertyDetail() {
    hiltViewModel<PropertyDetailViewModel>().subscribeToState { _, state ->
        DynamicPageLayout(state)
    }
}
