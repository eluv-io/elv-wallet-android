package app.eluvio.wallet.screens.property

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.VideoPlayer
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.property.rows.BannerSection
import app.eluvio.wallet.screens.property.rows.CarouselSection
import app.eluvio.wallet.screens.property.rows.DescriptionSection
import app.eluvio.wallet.screens.property.rows.TitleSection
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.compose.icons.Eluvio
import app.eluvio.wallet.util.compose.icons.Search
import app.eluvio.wallet.util.compose.icons.Switcher
import app.eluvio.wallet.util.logging.Log
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DynamicPageLayout(state: DynamicPageLayoutState) {
    if (state.isEmpty()) {
        DelayedFullscreenLoader()
        return
    }
    val bgImage = @Composable {
        if (state.backgroundImageUrl != null) {
            AsyncImage(
                model = state.backgroundImageUrl,
                contentScale = ContentScale.FillWidth,
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    if (state.backgroundVideo != null) {
        VideoPlayer(
            mediaSource = state.backgroundVideo,
            modifier = Modifier.fillMaxSize(),
            fallback = bgImage
        )
    } else {
        bgImage()
    }

    val listFocusRequester = remember { FocusRequester() }
    val scrollState = rememberLazyListState()
    TopActionRow(state, scrollState, listFocusRequester)

    LazyColumn(
        state = scrollState,
        contentPadding = PaddingValues(top = 50.dp, bottom = 32.dp),
        modifier = Modifier.focusRequester(listFocusRequester)
    ) {
        sections(state.sections)
    }
}

/**
 * Renders the [sections] inside a [LazyColumn].
 */
fun LazyListScope.sections(
    sections: List<DynamicPageLayoutState.Section>,
) {
    sections.forEach { section ->
        item(contentType = section::class) {
            when (section) {
                is DynamicPageLayoutState.Section.Banner -> BannerSection(
                    item = section,
                )

                is DynamicPageLayoutState.Section.Carousel -> CarouselSection(
                    item = section,
                )

                is DynamicPageLayoutState.Section.Description -> DescriptionSection(item = section)
                is DynamicPageLayoutState.Section.Title -> TitleSection(item = section)
            }
        }
    }
}

@Composable
private fun TopActionRow(
    state: DynamicPageLayoutState,
    listScrollState: LazyListState,
    listFocusRequester: FocusRequester,
) {
    val actionButtons = buildList<@Composable () -> Unit> {
        state.propertyLinks
            .takeIf { it.size > 1 }
            ?.let { add @Composable { PropertySwitcher(it) } }

        state.searchNavigationEvent
            ?.let { add @Composable { SearchButton(it) } }
    }
    var firstFocus by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val offset by remember(listScrollState) {
        derivedStateOf {
            if (listScrollState.firstVisibleItemIndex == 0) {
                listScrollState.firstVisibleItemScrollOffset
            } else {
                // This assumes that the first item in the list is tall enough to make the search
                // button completely scroll off the screen
                Int.MAX_VALUE
            }
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp, alignment = Alignment.End),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 37.dp, end = 47.dp)
            .offset { IntOffset(0, -offset) }
            .focusProperties { down = listFocusRequester }
            .onFocusChanged {
                if (it.hasFocus) {
                    if (firstFocus) {
                        // Prevent Search button from getting focus before other page elements
                        Log.d("Skipping TopActionRow focus, moving down.")
                        firstFocus = false
                        listFocusRequester.requestFocus()
                    } else {
                        // When search button gains focus, make sure list is scrolled to the top
                        scope.launch {
                            listScrollState.animateScrollToItem(0)
                        }
                    }
                }
            },
    ) {
        if (actionButtons.isEmpty()) {
            // Dummy element to capture focus at the top of the page
            Surface(onClick = { }, content = {}, modifier = Modifier.size(0.dp))
        } else {
            actionButtons.forEach { it() }
        }
    }
}

@Composable
private fun SearchButton(searchNavigationEvent: NavigationEvent) {
    val navigator = LocalNavigator.current
    ActionButton(
        icon = Icons.Eluvio.Search,
        onClick = { navigator(searchNavigationEvent) },
        contentDescription = "Search"
    )
}

@Composable
private fun PropertySwitcher(links: List<DynamicPageLayoutState.PropertyLink>) {
    var expanded by remember { mutableStateOf(false) }
    ActionButton(
        icon = Icons.Eluvio.Switcher,
        onClick = { expanded = true },
        contentDescription = "Switch Properties"
    ) {
        val navigator = LocalNavigator.current
        if (expanded) {
            val yOffset = LocalDensity.current.run { 10.dp.roundToPx() }
            val positionProvider = remember(yOffset) { LeftAlignedPopupPositionProvider(yOffset) }
            Popup(
                properties = PopupProperties(focusable = true),
                popupPositionProvider = positionProvider,
                onDismissRequest = { expanded = false }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .background(Color(0xFFa3a3a3), shape = MaterialTheme.shapes.large)
                        .padding(vertical = 10.dp)
                ) {
                    links.forEach { property ->
                        Surface(
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF2D2D2D),
                                focusedContainerColor = Color.White,
                                focusedContentColor = Color(0xFF2d2d2d),
                            ),
                            onClick = {
                                expanded = false
                                if (!property.isCurrent) {
                                    navigator(
                                        PropertyDetailDestination(
                                            property.id,
                                            propertyLinks = ArrayList(links)
                                        ).asReplace()
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 40.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.alpha(if (property.isCurrent) 1f else 0f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(property.name, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

class LeftAlignedPopupPositionProvider(private val yOffset: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return IntOffset(
            anchorBounds.left - popupContentSize.width + anchorBounds.width,
            anchorBounds.top + anchorBounds.height + yOffset
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF2d2d2d),
            contentColor = Color(0xFFC7C7C8),
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF2d2d2d),
        ),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(Color.White, 10.dp)),
        modifier = Modifier.size(30.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
        )
        content()
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun DynamicPageLayoutPreview() = EluvioThemePreview {
    DynamicPageLayout(
        DynamicPageLayoutState(
            searchNavigationEvent = NavigationEvent.GoBack,
            propertyLinks = listOf(
                DynamicPageLayoutState.PropertyLink(
                    id = "1",
                    name = "Link 1",
                    isCurrent = true,
                ),
                DynamicPageLayoutState.PropertyLink(
                    id = "2",
                    name = "Link 2",
                    isCurrent = false,
                ),
            ),
            sections = listOf(
                DynamicPageLayoutState.Section.Title("1", AnnotatedString("Title")),
                DynamicPageLayoutState.Section.Banner("2", "https://foo.com/image.jpg"),
                DynamicPageLayoutState.Section.Description("3", AnnotatedString("Description")),
                DynamicPageLayoutState.Section.Carousel(
                    permissionContext = PermissionContext(propertyId = "p", sectionId = "4"),
                    displaySettings = SimpleDisplaySettings(
                        title = "Carousel",
                        subtitle = "Subtitle",
                        displayFormat = DisplayFormat.CAROUSEL,
                    ),
                    items = listOf(
                        DynamicPageLayoutState.CarouselItem.Media(
                            permissionContext = PermissionContext(propertyId = "property1"),
                            entity = MediaEntity().apply {
                                id = "1"
                                name = "Media 1"
                                mediaType = "image"
                            },
                        ),
                        DynamicPageLayoutState.CarouselItem.RedeemableOffer(
                            permissionContext = PermissionContext(propertyId = "property1"),
                            offerId = "1",
                            name = "Offer 1",
                            fulfillmentState = RedeemableOfferEntity.FulfillmentState.AVAILABLE,
                            contractAddress = "0x123",
                            tokenId = "1",
                            imageUrl = "https://via.placeholder.com/150",
                            animation = null
                        )
                    )
                )
            )
        )
    )
}
