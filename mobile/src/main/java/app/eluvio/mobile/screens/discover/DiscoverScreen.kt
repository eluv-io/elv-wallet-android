package app.eluvio.mobile.screens.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.eluvio.wallet.screens.dashboard.discover.DiscoverViewModel
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage

/**
 * Entry body for [app.eluvio.mobile.navigation.DiscoverRoute]: binds [DiscoverViewModel] state
 * to the stateless overload below.
 */
@Composable
internal fun DiscoverScreen() {
    val vm: DiscoverViewModel = hiltViewModel()
    vm.subscribeToState { _, state ->
        DiscoverScreen(state = state, onPropertyClick = vm::onPropertyClicked, onRetry = vm::retry)
    }
}

@Composable
fun DiscoverScreen(
    state: DiscoverViewModel.State,
    onPropertyClick: (DiscoverViewModel.State.Property) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.showRetryButton -> RetryColumn(onRetry, Modifier.align(Alignment.Center))
            else -> PropertyGrid(state.properties, onPropertyClick)
        }
    }
}

@Composable
private fun PropertyGrid(
    properties: List<DiscoverViewModel.State.Property>,
    onPropertyClick: (DiscoverViewModel.State.Property) -> Unit,
) {
    // Combine status-bar inset with the 16dp vertical padding so first row clears the bar
    // at rest but items can scroll under it.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = statusBarTop + 16.dp,
            bottom = 16.dp,
        ),
    ) {
        items(properties, key = { it.id }) { property ->
            PropertyCard(property, onClick = { onPropertyClick(property) })
        }
    }
}

@Composable
private fun PropertyCard(
    property: DiscoverViewModel.State.Property,
    onClick: () -> Unit,
) {
    var imageFailed by remember(property.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            val image = property.cardImage
            if (image != null && !imageFailed) {
                AsyncImage(
                    model = image,
                    contentDescription = property.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true },
                )
            } else {
                Text(
                    text = property.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun RetryColumn(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't load properties", modifier = Modifier.padding(bottom = 12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

private const val GRID_COLUMNS = 2
