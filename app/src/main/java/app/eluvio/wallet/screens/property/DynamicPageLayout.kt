package app.eluvio.wallet.screens.property

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.property.rows.BannerSection
import app.eluvio.wallet.screens.property.rows.CarouselSection
import app.eluvio.wallet.screens.property.rows.DescriptionSection
import app.eluvio.wallet.screens.property.rows.TitleSection
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.logging.Log
import coil.compose.AsyncImage

@Composable
fun DynamicPageLayout(state: DynamicPageLayoutState) {
    if (state.isEmpty()) {
        DelayedFullscreenLoader()
        return
    }
    if (state.backgroundImagePath != null) {
        val url = state.urlForPath(state.backgroundImagePath)
        AsyncImage(
            model = url,
            contentScale = ContentScale.FillWidth,
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize()
        )
    }
    // Used to prevent Search button from getting focus before other page elements
    LazyColumn {
        item(contentType = "search", key = "search") {
            if (state.searchNavigationEvent != null) {
                var firstFocus by rememberSaveable { mutableStateOf(true) }
                val focusManager = LocalFocusManager.current
                SearchButton(
                    searchNavigationEvent = state.searchNavigationEvent,
                    modifier = Modifier.onFocusChanged {
                        if (it.isFocused && firstFocus) {
                            Log.d("Skipping Search button focus, moving down.")
                            firstFocus = false
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    })
            } else if (state.captureTopFocus) {
                // Empty item to make top of list focusable
                Spacer(
                    Modifier
                        .height(32.dp)
                        .focusable()
                )
            }
        }

        sections(state.sections, state.imagesBaseUrl)

        spacer(height = 32.dp)
    }
}

/**
 * Renders the [sections] inside a [LazyColumn].
 */
fun LazyListScope.sections(
    sections: List<DynamicPageLayoutState.Section>,
    imagesBaseUrl: String?
) {
    sections.forEach { section ->
        item(contentType = section::class) {
            when (section) {
                is DynamicPageLayoutState.Section.Banner -> BannerSection(
                    item = section,
                    imagesBaseUrl
                )

                is DynamicPageLayoutState.Section.Carousel -> CarouselSection(
                    item = section,
                    imagesBaseUrl
                )

                is DynamicPageLayoutState.Section.Description -> DescriptionSection(item = section)
                is DynamicPageLayoutState.Section.Title -> TitleSection(item = section)
            }
        }
    }
}

@Composable
private fun LazyItemScope.SearchButton(
    searchNavigationEvent: NavigationEvent,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.fillParentMaxWidth(),
    ) {
        val navigator = LocalNavigator.current
        Surface(
            onClick = { navigator(searchNavigationEvent) },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            modifier = modifier
                .padding(8.dp)
                .size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun DynamicPageLayoutPreview() = EluvioThemePreview {
    DynamicPageLayout(
        DynamicPageLayoutState(
            searchNavigationEvent = NavigationEvent.GoBack,
            sections = listOf(
                DynamicPageLayoutState.Section.Title("1", AnnotatedString("Title")),
                DynamicPageLayoutState.Section.Banner("2", "https://foo.com/image.jpg"),
                DynamicPageLayoutState.Section.Description("3", AnnotatedString("Description")),
                DynamicPageLayoutState.Section.Carousel(
                    sectionId = "4",
                    title = "Carousel",
                    subtitle = "Subtitle",
                    items = listOf(
                        DynamicPageLayoutState.CarouselItem.Media(
                            entity = MediaEntity().apply {
                                id = "1"
                                name = "Media 1"
                                mediaType = "image"
                            },
                            propertyId = "property1"
                        ),
                        DynamicPageLayoutState.CarouselItem.RedeemableOffer(
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
