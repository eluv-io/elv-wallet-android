package app.eluvio.wallet.screens.property.items

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Surface
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.property.DynamicPageLayoutState

@Composable
fun SubpropertyCard(item: DynamicPageLayoutState.CarouselItem.SubpropertyLink, cardHeight: Dp) {
    val navigator = LocalNavigator.current
    Surface(
        onClick = { navigator(PropertyDetailDestination(item.subpropertyId).asPush()) },
        modifier = Modifier.height(cardHeight)
    ) {
        ShimmerImage(model = item.imageUrl, contentDescription = null)
    }
}
