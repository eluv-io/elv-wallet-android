package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickDirection
import app.eluvio.wallet.screens.common.MediaItemCard
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.disabledItemAlpha
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.thenIf

@Composable
fun CarouselItemCard(carouselItem: CarouselItem, cardHeight: Dp, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val onClick: () -> Unit = remember {
        {
            carouselItem.onClickDirection()?.let {
                navigator(it.asPush())
            }
        }
    }
    when (carouselItem) {
        is CarouselItem.Media -> Column(modifier = modifier.width(IntrinsicSize.Min)) {
            val entity = carouselItem.entity
            MediaItemCard(
                entity,
                displayOverrides = carouselItem.displayOverrides,
                cardHeight = cardHeight,
                permissionContext = carouselItem.permissionContext
            )
            Spacer(Modifier.height(10.dp))
            val title = carouselItem.displayOverrides?.title ?: entity.name
            Text(
                title,
                style = MaterialTheme.typography.label_24.copy(fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.thenIf(entity.isDisabled) {
                    alpha(MaterialTheme.colorScheme.disabledItemAlpha)
                }
            )
        }

        is CarouselItem.RedeemableOffer -> OfferCard(
            carouselItem,
            cardHeight,
            onClick
        )

        is CarouselItem.PageLink -> PageLinkCard(
            carouselItem,
            cardHeight,
            onClick
        )

        is CarouselItem.ExternalLink -> DisplaySettingsCard(
            displaySettings = carouselItem.displaySettings,
            cardHeight = cardHeight,
            onClick
        )

        is CarouselItem.ItemPurchase -> DisplaySettingsCard(
            displaySettings = carouselItem.displaySettings,
            cardHeight = cardHeight,
            onClick
        )

        is CarouselItem.BannerWrapper -> BannerItem(
            carouselItem,
            onClick = onClick,
            modifier
        )
    }
}

@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun CarouselItemCardPreview() = EluvioThemePreview {
    CarouselItemCard(
        carouselItem = CarouselItem.Media(
            permissionContext = PermissionContext(propertyId = "property"),
            entity = MediaEntity().apply {
                name = "this is a very very very very long title"
            },
        ), 120.dp
    )
}
