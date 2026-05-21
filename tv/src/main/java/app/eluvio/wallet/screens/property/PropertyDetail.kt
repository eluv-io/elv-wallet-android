package app.eluvio.wallet.screens.property

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.eluvio.wallet.util.subscribeToState

/**
 * See [DynamicPageLayout] for @Preview
 */
@Composable
fun PropertyDetail() {
    hiltViewModel<PropertyDetailViewModel>().subscribeToState { _, state ->
        DynamicPageLayout(state)
    }
}
