package app.eluvio.mobile.screens.property

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eluvio.mobile.R
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.Section
import app.eluvio.wallet.screens.property.PropertyDetailViewModel
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage

/**
 * Entry body for [app.eluvio.mobile.navigation.PropertyDetailNavArgs]: binds
 * [PropertyDetailViewModel] state to the stateless overload below and resolves carousel item
 * clicks into navigation events (only video is wired up today; everything else toasts).
 */
@Composable
internal fun PropertyDetailScreen(vm: PropertyDetailViewModel) {
    val context = LocalContext.current
    vm.subscribeToState { _, state ->
        PropertyDetailScreen(
            state = state,
            onItemClick = { item ->
                val real = (item as? CarouselItem.BannerWrapper)?.delegate ?: item
                if (real !is CarouselItem.Media) {
                    Toast.makeText(context, "Not supported yet: $real", Toast.LENGTH_SHORT).show()
                    return@PropertyDetailScreen
                }
                when (val target = real.entity.onClickTarget(real.permissionContext)) {
                    is VideoPlayerArgs -> vm.navigateTo(target.asPush())
                    null -> Toast.makeText(context, "No access to this item", Toast.LENGTH_SHORT)
                        .show()

                    else -> Toast.makeText(
                        context,
                        "Not supported yet: $target",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            },
            onNavigateUp = { vm.navigateTo(NavigationEvent.GoBack) },
        )
    }
}

/**
 * Compose body for [PropertyDetailFragment]. Caller is responsible for wrapping in
 * [app.eluvio.mobile.theme.EluvioMobileTheme] so `MaterialTheme.*` lookups resolve.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    state: DynamicPageLayoutState,
    onItemClick: (CarouselItem) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val logo = state.headerLogo
                    if (logo != null) {
                        AsyncImage(
                            model = logo,
                            contentDescription = null,
                            modifier = Modifier.height(32.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = state.sections,
                key = { it.sectionId },
                contentType = { it::class },
            ) { section ->
                when (section) {
                    is Section.Title -> TextSection(
                        section.text,
                        MaterialTheme.typography.headlineMedium,
                    )

                    is Section.Description -> TextSection(
                        section.text,
                        MaterialTheme.typography.bodyMedium,
                    )

                    is Section.SectionHeader -> TextSection(
                        section.text,
                        MaterialTheme.typography.titleMedium,
                    )

                    is Section.Banner -> BannerSection(section.imageUrl)
                    is Section.Carousel -> CarouselRow(section.items, onItemClick)
                }
            }
        }
    }
}

@Composable
private fun TextSection(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
) {
    Text(
        text = text,
        style = style,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun BannerSection(imageUrl: Any?) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun CarouselRow(
    items: List<CarouselItem>,
    onClick: (CarouselItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { it.permissionContext.sectionItemId ?: it.permissionContext.toString() },
        ) { item ->
            CarouselCard(item, onClick)
        }
    }
}

@Composable
private fun CarouselCard(
    item: CarouselItem,
    onClick: (CarouselItem) -> Unit,
) {
    val card = item.toCard()
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(item) },
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            AsyncImage(
                model = card.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (item.isVideo()) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .alpha(0.75f),
                )
            }
            if (item.needsInaccessibleOverlay()) {
                Box(Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)))
            }
        }
        Text(
            text = card.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
        )
    }
}
