package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import coil.compose.AsyncImage

@Composable
fun BannerSection(
    item: DynamicPageLayoutState.Section.Banner,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = item.imageUrl,
        contentDescription = "Logo",
        modifier
            .padding(
                start = Overscan.horizontalPadding,
                end = Overscan.horizontalPadding,
                bottom = 40.dp
            )
            // Matches tvOS, which pins the hero logo to a fixed height (180pt) and lets the width
            // follow the aspect ratio. Unlike tvOS, we also cap the width, so that very wide logos
            // scale down instead of running off the screen.
            .height(90.dp)
            .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.7f)
    )
}
