package app.eluvio.mobile.screens.property

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eluvio.mobile.R
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.HeroAction
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.Section
import app.eluvio.wallet.screens.property.PropertyDetailViewModel
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import app.eluvio.wallet.util.compose.LocalCardTheme
import app.eluvio.wallet.util.compose.cardBackground
import app.eluvio.wallet.util.compose.cardTitleAlign
import app.eluvio.wallet.util.compose.toBrush
import app.eluvio.wallet.util.compose.cardBorder
import app.eluvio.wallet.util.compose.cardShape
import app.eluvio.wallet.util.compose.hasBorder
import app.eluvio.wallet.util.compose.imageSaturation
import app.eluvio.wallet.util.compose.saturationFilter
import app.eluvio.wallet.util.subscribeToState
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

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
            onHeroActionClick = { vm.navigateTo(it.navigationEvent) },
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
    onHeroActionClick: (HeroAction) -> Unit,
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
        // Sections are fetched after the page itself resolves, so until they land there's
        // nothing to draw but the toolbar. Mirrors `:tv`'s DynamicPageLayout, which shows
        // DelayedFullscreenLoader while the state is still empty.
        if (state.sections.isEmpty()) {
            DelayedLoader(Modifier.padding(padding))
            return@Scaffold
        }
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
                    is Section.Carousel -> CarouselRow(section, onItemClick)
                    is Section.HeroActions -> HeroActionsRow(section.actions, onHeroActionClick)
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
private fun HeroActionsRow(
    actions: List<HeroAction>,
    onClick: (HeroAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        actions.forEach { action ->
            Button(
                onClick = { onClick(action) },
                shape = RoundedCornerShape(action.cornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = action.backgroundColor,
                    contentColor = action.textColor,
                ),
                border = action.borderColor?.let { BorderStroke(1.dp, it) },
            ) {
                Text(action.text)
            }
        }
    }
}

@Composable
private fun CarouselRow(
    section: Section.Carousel,
    onClick: (CarouselItem) -> Unit,
) {
    CompositionLocalProvider(LocalCardTheme provides section.cardTheme) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = section.items,
                key = { it.permissionContext.sectionItemId ?: it.permissionContext.toString() },
            ) { item ->
                CarouselCard(
                    item,
                    titleAlign = section.displaySettings.cardTitleAlign,
                    onClick = onClick,
                )
            }
        }
    }
}

@Composable
private fun CarouselCard(
    item: CarouselItem,
    titleAlign: TextAlign?,
    onClick: (CarouselItem) -> Unit,
) {
    val card = item.toCard()
    // Mobile cards are always square. The theme's "mobile_state" isn't implemented yet, so cards
    // always render in the "inactive" state.
    val theme = LocalCardTheme.current
    val shape = theme.cardShape(AspectRatio.SQUARE, RoundedCornerShape(8.dp))
    val border = theme?.takeIf { it.hasBorder }?.cardBorder(focused = false)
    val background = theme.cardBackground(focused = false)
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(item) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(AspectRatio.SQUARE)
                .clip(shape)
                .then(if (background != null) Modifier.background(background.toBrush()) else Modifier)
                .then(if (border != null) Modifier.border(border, shape) else Modifier),
        ) {
            AsyncImage(
                model = card.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = saturationFilter(theme.imageSaturation(focused = false)),
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
        if (titleAlign != null) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = titleAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }
    }
}

/**
 * Fullscreen spinner, held back briefly so a fast load doesn't flash it on screen.
 * Mobile equivalent of `:tv`'s DelayedFullscreenLoader, which uses the TV-only spinner.
 */
@Composable
private fun DelayedLoader(modifier: Modifier = Modifier) {
    var showLoader by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADER_DELAY_MS)
        showLoader = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showLoader) {
            CircularProgressIndicator()
        }
    }
}

private const val LOADER_DELAY_MS = 400L
