package app.eluvio.wallet.screens.nftdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.entities.MediaCollectionEntity
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.MediaSectionEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity.FulfillmentState
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.ImageCard
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.WrapContentText
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.destinations.RedeemDialogDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.LocalSurfaceScale
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.theme.onRedeemTagSurface
import app.eluvio.wallet.theme.redeemTagSurface
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.focusRestorer
import app.eluvio.wallet.util.compose.rememberToaster
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.findActivity
import app.eluvio.wallet.util.subscribeToState
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import io.realm.kotlin.ext.realmListOf

@MainGraph
@Destination(navArgsDelegate = NftDetailArgs::class)
@Composable
fun NftDetail() {
    val toaster = rememberToaster()
    hiltViewModel<NftDetailViewModel>().subscribeToState(
        onEvent = { event ->
            if (event is Events.NftNotFound) {
                toaster.toast("You don't own this NFT")
            }
        },
        onState = { _, state ->
            NftDetail(state)
        }
    )
}

@Composable
private fun NftDetail(state: NftDetailViewModel.State) {
    if (state.isEmpty()) {
        DelayedFullscreenLoader()
        return
    }
    if (state.backgroundImage != null) {
        AsyncImage(
            model = state.backgroundImage,
            contentScale = ContentScale.Crop,
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize()
        )
    }
    val lazyColumnFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    if (state.backLinkUrl != null) {
        BackToThirdPartyButton(
            state.backButtonLogo,
            Modifier
                .focusRequester(backButtonFocusRequester)
                .focusProperties { down = lazyColumnFocusRequester }
        )
    }
    Column {
        TvLazyColumn(
            pivotOffsets = PivotOffsets(0.6f),
            modifier = Modifier
                .focusRequester(lazyColumnFocusRequester)
                .focusProperties { up = backButtonFocusRequester })
        {
            spacer(height = 32.dp)
            item {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.title_62,
                    modifier = Modifier.padding(start = Overscan.horizontalPadding, end = 260.dp)
                )
                DescriptionText(text = state.subtitle)
            }
            item {
                FeaturedMediaAndOffersRow(state)
            }

            state.sections.forEach { section ->
                section.name
                    .takeIf { it.isNotEmpty() }
                    ?.let { sectionName ->
                        item(
                            key = "${section.id}_$sectionName",
                            contentType = "section_name"
                        ) {
                            Text(
                                sectionName,
                                style = MaterialTheme.typography.body_32,
                                modifier = Modifier.padding(
                                    horizontal = Overscan.horizontalPadding,
                                    vertical = 16.dp
                                )
                            )
                        }
                    }
                // Only show collections that have at least one visible media item
                val collections = section.collections
                    .filter { collection -> collection.media.any { !it.shouldBeHidden() } }
                items(collections, contentType = { "collection" }) { collection ->
                    if (collection.name.isNotEmpty()) {
                        Text(
                            collection.name,
                            style = MaterialTheme.typography.body_32,
                            modifier = Modifier.padding(
                                horizontal = Overscan.horizontalPadding,
                                vertical = 16.dp
                            )
                        )
                    }
                    MediaItemsRow(collection.media)
                }
            }

            spacer(height = 32.dp)
        }
    }
}

@Composable
private fun BackToThirdPartyButton(
    backButtonLogo: String?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, end = 32.dp)
    ) {
        val border = Border(
            BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondaryContainer),
            shape = MaterialTheme.shapes.extraSmall
        )
        val context = LocalContext.current
        Surface(
            onClick = { context.findActivity()?.finish() },
            border = ClickableSurfaceDefaults.border(border = border),
            scale = LocalSurfaceScale.current,
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.2f),
                focusedContainerColor = Color.Black
            ),
            modifier = Modifier.size(width = 160.dp, height = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // TODO: handle logo missing / error loading
                val text = if (backButtonLogo != null) "Back to" else "Go Back"
                Text(
                    text,
                    style = MaterialTheme.typography.label_24.copy(fontSize = 16.sp),
                )
                backButtonLogo?.let { logo ->
                    // scale logo request from server to save bandwidth
                    val url = logo + "?h=" + LocalDensity.current.run { 20.dp.roundToPx() }
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        placeholder = rememberVectorPainter(
                            defaultWidth = 50.dp,
                            defaultHeight = 20.dp,
                            autoMirror = false,
                        ) { _, _ ->
                            // Empty vector painter to take up the space we expect the logo to be while we load it
                        },
                        modifier = Modifier
                            .height(20.dp)
                            .padding(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptionText(text: AnnotatedString) {
    var isClickable by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    Text(
        text = text,
        style = MaterialTheme.typography.body_32,
        maxLines = if (expanded) Int.MAX_VALUE else 10,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
            // Only clickable if there's actually overflow.
            isClickable = expanded || textLayoutResult.hasVisualOverflow
        },
        modifier = Modifier
            .clickable(enabled = isClickable, onClick = { expanded = !expanded })
            .padding(
                start = Overscan.horizontalPadding,
                end = 260.dp,
                top = 16.dp,
                bottom = 16.dp
            )
    )
}

@Composable
private fun FeaturedMediaAndOffersRow(state: NftDetailViewModel.State) {
    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        spacer(width = 28.dp)
        items(state.featuredMedia) { item -> MediaItemCard(item, cardHeight = CARD_HEIGHT) }
        items(state.redeemableOffers) { item ->
            val navigator = LocalNavigator.current
            OfferCard(item) {
                navigator(
                    RedeemDialogDestination(
                        item.contractAddress,
                        item.tokenId,
                        item.offerId
                    ).asPush()
                )
            }
        }
        spacer(width = 28.dp)
    }
}

@Composable
private fun MediaItemsRow(media: List<MediaEntity>) {
    val items = media.filter { !it.shouldBeHidden() }
    if (items.isNotEmpty()) {
        val firstItemFocusRequester = remember { FocusRequester() }
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.focusRestorer(firstItemFocusRequester)
        ) {
            spacer(width = 28.dp)
            itemsIndexed(items) { index, media ->
                MediaItemCard(
                    media,
                    cardHeight = CARD_HEIGHT,
                    modifier = Modifier.thenIf(index == 0) { focusRequester(firstItemFocusRequester) })
            }
            spacer(width = 28.dp)
        }
    }
}

@Composable
private fun OfferCard(item: NftDetailViewModel.State.Offer, onClick: () -> Unit) {
    // It's possible to layer this Text on top of the card (with explicit zIndex modifiers, see:
    // https://issuetracker.google.com/issues/291642442), but then it won't scale right when
    // the card is focused.
    // So instead we draw it both in the focused overlay, and unfocused overlay.
    val rewardOverlay = remember<@Composable BoxScope.() -> Unit> {
        {
            val text = when (item.fulfillmentState) {
                FulfillmentState.AVAILABLE, FulfillmentState.UNRELEASED -> "REWARD"
                FulfillmentState.EXPIRED -> "EXPIRED REWARD"
                FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER -> "CLAIMED REWARD"
            }
            Text(
                text = text,
                style = MaterialTheme.typography.label_24,
                color = MaterialTheme.colorScheme.onRedeemTagSurface,
                modifier = Modifier
                    .padding(10.dp)
                    .background(
                        MaterialTheme.colorScheme.redeemTagSurface,
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 0.dp)
                    .align(Alignment.BottomCenter)
            )
            when (item.fulfillmentState) {
                FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER,
                FulfillmentState.EXPIRED -> {
                    // Gray out unavailable offers
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                }

                else -> {
                    /* no-op */
                }
            }
        }
    }
    val offerTitle = remember<@Composable BoxScope.() -> Unit> {
        {
            WrapContentText(
                text = item.name,
                style = MaterialTheme.typography.body_32,
                // TODO: get this from theme
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 10.dp, vertical = 20.dp)
            )
        }
    }
//    if (item.animation != null) {
//        val interactionSource = remember { MutableInteractionSource() }
//        val focused by interactionSource.collectIsFocusedAsState()
//        val focusedBorder =
//            Border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondaryContainer))
//        Surface(
//            onClick = onClick,
//            interactionSource = interactionSource,
//            border = ClickableSurfaceDefaults.border(focusedBorder = focusedBorder),
//            scale = LocalSurfaceScale.current,
//            modifier = Modifier.size(150.dp)
//        ) {
//            VideoPlayer(
//                mediaSource = item.animation,
//                modifier = Modifier
//                    .size(150.dp)
//                    .dimContent(dim = focused)
//            )
//            if (focused) {
//                offerTitle()
//            }
//            rewardTag()
//        }
//    } else {
    ImageCard(
        imageUrl = item.imageUrl,
        contentDescription = item.name,
        onClick = onClick,
        modifier = Modifier.size(CARD_HEIGHT),
        focusedOverlay = {
            offerTitle()
            rewardOverlay()
        },
        unFocusedOverlay = rewardOverlay
    )
//    }
}

class BackLinkParameterProvider : PreviewParameterProvider<String?> {
    override val values = sequenceOf(null, "http://3rdparty.app")
}

private val CARD_HEIGHT = 170.dp

@Composable
@Preview(device = Devices.TV_720p)
private fun NftDetailPreview(@PreviewParameter(BackLinkParameterProvider::class) backlink: String) =
    EluvioThemePreview {
        NftDetail(
            NftDetailViewModel.State(
                backLinkUrl = backlink,
                title = "Superman",
                subtitle = AnnotatedString(
                    """
            Superman Web3 Movie Experience includes:
            Immersive menus featuring Fortress of Solitude, Metropolis, and Lex Luthor’s Lair
            Superman The Movie (Theatrical version) • Hours of special features*
            Curated image galleries • Hidden digital easter eggs
            A Voucher Code** for DC3 Super Power Pack: Series Superman from DC NFT Marketplace
        """.trimIndent()
                ),
                featuredMedia = listOf(
                    MediaEntity().apply {
                        name = "Feature Film"
                        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
                    },
                ),
                redeemableOffers = listOf(
                    NftDetailViewModel.State.Offer(
                        "_id",
                        "NFT reward",
                        fulfillmentState = FulfillmentState.AVAILABLE,
                        "https://via.placeholder.com/150",
                        "contractAddr",
                        "token_1",
                        animation = null,
                    )
                ),
                sections = listOf(
                    MediaSectionEntity().apply {
                        name = "Section 1"
                        collections = realmListOf(
                            MediaCollectionEntity().apply {
                                name = "Movies"
                                media = realmListOf(
                                    MediaEntity().apply {
                                        name = "Superman 1"
                                        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
                                    },
                                    MediaEntity().apply {
                                        name = "Superman 2"
                                        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
                                    },
                                )
                            },
                            MediaCollectionEntity().apply {
                                name = "Extras"
                                media = realmListOf(
                                    MediaEntity().apply {
                                        name = "Superman 2052 Poster"
                                        mediaType = MediaEntity.MEDIA_TYPE_IMAGE
                                    },
                                    MediaEntity().apply {
                                        name = "Man of Steel Trailer"
                                        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
                                    },
                                )
                            }
                        )
                    })
            )
        )
    }

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun OfferCardAvailablePreview() = EluvioThemePreview {
    OfferCard(
        item = NftDetailViewModel.State.Offer(
            "_id",
            "NFT reward",
            fulfillmentState = FulfillmentState.AVAILABLE,
            "https://via.placeholder.com/150",
            "contractAddr",
            "token_1",
            animation = null,
        ),
        onClick = {}
    )
}

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun OfferCardExpiredPreview() = EluvioThemePreview {
    OfferCard(
        item = NftDetailViewModel.State.Offer(
            "_id",
            "NFT reward",
            fulfillmentState = FulfillmentState.EXPIRED,
            "https://via.placeholder.com/150",
            "contractAddr",
            "token_1",
            animation = null,
        ),
        onClick = {}
    )
}

@Preview(widthDp = 200, heightDp = 200)
@Composable
private fun OfferCardClaimedByAnotherPreview() = EluvioThemePreview {
    OfferCard(
        item = NftDetailViewModel.State.Offer(
            "_id",
            "NFT reward",
            fulfillmentState = FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER,
            "https://via.placeholder.com/150",
            "contractAddr",
            "token_1",
            animation = null,
        ),
        onClick = {}
    )
}
