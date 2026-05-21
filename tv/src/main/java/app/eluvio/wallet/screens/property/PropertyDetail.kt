package app.eluvio.wallet.screens.property

import androidx.compose.runtime.Composable
import app.eluvio.wallet.util.subscribeToState

/**
 * See [DynamicPageLayout] for @Preview
 */
@Composable
fun PropertyDetail(vm: PropertyDetailViewModel) {
    vm.subscribeToState { _, state ->
        DynamicPageLayout(state)
    }
}
