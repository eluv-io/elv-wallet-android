package app.eluvio.wallet.screens.property.items

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.CardSize
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.data.entities.v2.display.withOverrides
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.common.DIM_ANIMATION_MILLIS
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.disabledItemAlpha
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.LocalCardTheme
import app.eluvio.wallet.util.compose.isCircular
import app.eluvio.wallet.util.compose.thenIf

/**
 * Card heights per [CardSize], as defined by design (in 1080p pixels, which are 2x dp).
 * Portrait cards are taller than the rest, which all share the same height.
 */
fun CardSize.cardHeight(aspectRatio: Float?): Dp = when {
    aspectRatio == AspectRatio.POSTER -> when (this) {
        CardSize.EXTRA_SMALL -> 148.dp
        CardSize.SMALL -> 174.dp
        CardSize.MEDIUM -> 200.dp
        CardSize.LARGE -> 243.dp
        CardSize.EXTRA_LARGE -> 289.dp
    }

    else -> when (this) {
        CardSize.EXTRA_SMALL -> 85.dp
        CardSize.SMALL -> 100.dp
        CardSize.MEDIUM -> 118.dp
        CardSize.LARGE -> 140.dp
        CardSize.EXTRA_LARGE -> 167.dp
    }
}

/**
 * The aspect ratio this item's card will be rendered at, when it has one.
 * Only used to pick the card's height - the cards themselves resolve their own aspect ratio.
 */
val CarouselItem.aspectRatio: Float?
    get() = when (this) {
        is CarouselItem.Media -> entity.requireDisplaySettings().withOverrides(displayOverrides)
        is CarouselItem.ExternalLink -> displaySettings
        is CarouselItem.ItemPurchase -> displaySettings
        is CarouselItem.PageLink -> displaySettings
        is CarouselItem.RedeemableOffer,
        is CarouselItem.VisualOnly,
        is CarouselItem.BannerWrapper -> null
    }?.thumbnailUrlAndRatio?.second

@Composable
fun CarouselItemCard(
    carouselItem: CarouselItem,
    cardHeight: Dp,
    /** How to align the title under the card, or null when the section shows no titles. */
    titleAlign: TextAlign?,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    val onClick: () -> Unit = remember {
        {
            carouselItem.onClickTarget()?.let {
                navigator(it.asPush())
            }
        }
    }
    // Only read by the Media branch, but the Column needs it before its content runs.
    var cardFocused by remember { mutableStateOf(false) }
    when (carouselItem) {
        is CarouselItem.Media -> Column(
            modifier
                .width(IntrinsicSize.Min)
                .onFocusChanged { cardFocused = it.hasFocus }
        ) {
            val entity = carouselItem.entity
            MediaItemCard(
                entity,
                displayOverrides = carouselItem.displayOverrides,
                cardHeight = cardHeight,
                permissionContext = carouselItem.permissionContext,
                forceDisabled = carouselItem.forceDisabled,
                playbackProgress = carouselItem.playbackProgress,
            )
            if (titleAlign != null) {
                Spacer(Modifier.height(10.dp))
                val title = carouselItem.displayOverrides?.title ?: entity.name
                // Focus draws the card's own title over the image, making this copy a duplicate.
                // Circular cards don't react to focus, and disabled ones show an error instead of
                // a title, so both keep the copy underneath.
                val duplicate = cardFocused && !carouselItem.forceDisabled && !entity.isDisabled &&
                        !LocalCardTheme.current.isCircular(carouselItem.aspectRatio)
                val titleAlpha by animateFloatAsState(
                    targetValue = if (duplicate) 0f else 1f,
                    animationSpec = tween(durationMillis = DIM_ANIMATION_MILLIS),
                    label = "cardTitleAlpha"
                )
                Text(
                    title,
                    style = MaterialTheme.typography.label_24.copy(fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = titleAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(titleAlpha)
                        .thenIf(entity.isDisabled) {
                            alpha(MaterialTheme.colorScheme.disabledItemAlpha)
                        }
                )
            }
        }

        is CarouselItem.RedeemableOffer -> OfferCard(
            carouselItem,
            cardHeight,
            onClick
        )

        is CarouselItem.PageLink -> PageLinkCard(
            carouselItem,
            cardHeight,
            titleAlign,
            onClick
        )

        is CarouselItem.ExternalLink -> DisplaySettingsCard(
            displaySettings = carouselItem.displaySettings,
            cardHeight = cardHeight,
            titleAlign = titleAlign,
            onClick = onClick,
        )

        is CarouselItem.ItemPurchase -> DisplaySettingsCard(
            displaySettings = carouselItem.displaySettings,
            cardHeight = cardHeight,
            titleAlign = titleAlign,
            onClick = onClick,
        )

        is CarouselItem.BannerWrapper -> BannerItem(
            carouselItem,
            onClick = onClick,
            modifier
        )

        is CarouselItem.VisualOnly -> {
            // We currently only support Banners for VisualOnly.
            // So this should be handled in the BannerWrapper case.
        }
    }
}

@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun CarouselItemCardPreview() = EluvioThemePreview {
    CarouselItemCard(
        carouselItem = CarouselItem.Media(
            permissionContext = PermissionContext(propertyId = "property"),
            forceDisabled = false,
            entity = MediaEntity().apply {
                name = "this is a very very very very long title"
            },
            playbackProgress = null,
        ), CardSize.MEDIUM.cardHeight(AspectRatio.SQUARE), titleAlign = TextAlign.Start
    )
}
