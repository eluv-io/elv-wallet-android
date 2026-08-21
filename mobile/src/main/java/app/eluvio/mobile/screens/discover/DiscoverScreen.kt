package app.eluvio.mobile.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.screens.dashboard.discover.DiscoverViewModel
import app.eluvio.wallet.util.subscribeToState
import coil3.compose.AsyncImage

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
    Box(modifier = modifier
        .fillMaxSize()
        .background(ScreenBackground)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.showRetryButton -> RetryColumn(onRetry, Modifier.align(Alignment.Center))
            else -> DiscoverRows(state.rows, onPropertyClick)
        }
    }
}

@Composable
private fun DiscoverRows(
    rows: List<DiscoverViewModel.State.Row>,
    onPropertyClick: (DiscoverViewModel.State.Property) -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        itemsIndexed(
            rows,
            // Rows have no server-side id, and featured rows have no title either.
            key = { index, row -> "$index:${row.title}" },
            contentType = { _, row -> row.featured },
        ) { index, row ->
            // The status-bar inset is padding *inside* the first row rather than on the list,
            // so a featured row's background band reaches up behind the status bar. Items
            // still scroll under the bar.
            val topPadding = if (index == 0) statusBarTop + 12.dp else 0.dp
            if (row.featured) {
                FeaturedRow(row.properties, topPadding, onPropertyClick)
            } else {
                PropertyRow(row, topPadding, onPropertyClick)
            }
        }
    }
}

/**
 * A featured row: one near-full-width card per Property, swiped horizontally, with the next
 * card peeking in from the edge and page dots underneath.
 */
@Composable
private fun FeaturedRow(
    properties: List<DiscoverViewModel.State.Property>,
    topPadding: Dp,
    onPropertyClick: (DiscoverViewModel.State.Property) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { properties.size })
    Column(
        Modifier
            .fillMaxWidth()
            .background(FeaturedSectionBackground)
            .padding(top = topPadding, bottom = 20.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = FEATURED_PEEK),
            pageSpacing = 12.dp,
        ) { page ->
            val property = properties[page]
            FeaturedCard(property, onClick = { onPropertyClick(property) })
        }
        if (properties.size > 1) {
            PagerDots(
                currentPage = pagerState.currentPage,
                pageCount = properties.size,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun FeaturedCard(
    property: DiscoverViewModel.State.Property,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(AspectRatio.POSTER)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable(onClick = onClick),
    ) {
        PropertyImage(property, property.featuredCardImage)
        // Scrim over the lower half, so the logo/title/description stay readable.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color(0xCC000000),
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            // Properties in a featured row ship a logo for this card. Only Properties
            // without one fall back to their title as text.
            val logo = property.featuredCardLogo
            val title = property.mainPageTitle
            if (logo != null) {
                AsyncImage(
                    model = logo,
                    contentDescription = property.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                )
            } else if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val description = property.mainPageDescription
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PagerDots(currentPage: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { page ->
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (page == currentPage) Color.White else Color(0x66FFFFFF))
            )
        }
    }
}

/** A titled row of Property cards. Rows aren't required to have a title. */
@Composable
private fun PropertyRow(
    row: DiscoverViewModel.State.Row,
    topPadding: Dp,
    onPropertyClick: (DiscoverViewModel.State.Property) -> Unit,
) {
    Column(
        Modifier
            .padding(top = topPadding)
            .padding(vertical = 8.dp)
    ) {
        if (row.title.isNotEmpty()) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(row.properties, key = { it.id }) { property ->
                PropertyCard(property, onClick = { onPropertyClick(property) })
            }
        }
    }
}

@Composable
private fun PropertyCard(
    property: DiscoverViewModel.State.Property,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(CARD_WIDTH)
            .aspectRatio(AspectRatio.POSTER)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .clickable(onClick = onClick),
    ) {
        PropertyImage(property, property.cardImage)
    }
}

/**
 * A Property's card art, falling back to its name when there's no image, or the image fails
 * to load.
 */
@Composable
private fun PropertyImage(property: DiscoverViewModel.State.Property, image: FabricUrl?) {
    var imageFailed by remember(property.id) { mutableStateOf(false) }
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        )
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

/** How much of the neighboring featured card peeks in from each edge. */
private val FEATURED_PEEK = 36.dp
private val CARD_WIDTH = 132.dp

// Neutral grays from the design: the featured section sits on a lighter band than the
// rows below it.
private val ScreenBackground = Color(0xFF1C1C1E)
private val FeaturedSectionBackground = Color(0xFF2C2C2E)
private val CardBackground = Color(0xFF232326)
