package app.eluvio.wallet.screens.home.temp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.navigation.DashboardTabsGraph
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination
import kotlin.math.roundToInt

@DashboardTabsGraph(start = true)
@Destination
@Composable
fun Discover() {
    hiltViewModel<DiscoverViewModel>().subscribeToState { _, state ->
        Discover(state)
    }
}

@Composable
private fun Discover(state: DiscoverViewModel.State) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.loading) {
            EluvioLoadingSpinner()
        } else if (state.properties.isEmpty()) {
            Text(stringResource(R.string.no_content_warning))
        } else {
            val navigator = LocalNavigator.current
            DiscoverGrid(
                state,
                onPropertyClicked = {
                    navigator(PropertyDetailDestination(it.id).asPush())
                }
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.DiscoverGrid(
    state: DiscoverViewModel.State,
    onPropertyClicked: (MediaPropertyEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val width by rememberUpdatedState(maxWidth)
    val horizontalPadding = 50.dp
    val cardSpacing = 20.dp
    val desiredCardWidth = 240.dp
    val columnCount by remember {
        derivedStateOf {
            val availableWidth = width - horizontalPadding
            val cardWidth = desiredCardWidth + cardSpacing
            return@derivedStateOf (availableWidth / cardWidth).roundToInt()
        }
    }
    val firstItemFocusRequester = remember { FocusRequester() }
    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(columnCount),
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalArrangement = Arrangement.spacedBy(cardSpacing),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pivotOffsets = PivotOffsets(0.1f),
        modifier = Modifier.focusRestorer(firstItemFocusRequester)
    ) {
        item(span = { TvGridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(10.dp))
        }
        itemsIndexed(state.properties) { index, property ->
            Surface(
                onClick = { onPropertyClicked(property) },
                modifier = Modifier.thenIf(index == 0) {
                    focusRequester(firstItemFocusRequester)
                }) {
                ShimmerImage(
                    model = "${state.baseUrl}/${property.image}",
                    contentDescription = property.name
                )
            }
        }
        item(span = { TvGridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun DiscoverPreview() = EluvioThemePreview {
    Discover(DiscoverViewModel.State())
}
