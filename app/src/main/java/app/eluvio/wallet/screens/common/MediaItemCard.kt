package app.eluvio.wallet.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.LiveVideoInfoEntity
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.getStartDateTimeString
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.DisplaySettingsEntity
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.data.entities.v2.display.withOverrides
import app.eluvio.wallet.data.entities.v2.permissions.PermissionBehavior
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.Navigator
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickDirection
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.button_24
import app.eluvio.wallet.theme.disabledItemAlpha
import app.eluvio.wallet.util.compose.Black
import app.eluvio.wallet.util.compose.requestInitialFocus
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant

typealias MediaClickHandler = (MediaEntity, PermissionContext?) -> Unit

@Composable
fun MediaItemCard(
    media: MediaEntity,
    modifier: Modifier = Modifier,
    displayOverrides: DisplaySettings? = null,
    permissionContext: PermissionContext? = null,
    onMediaItemClick: MediaClickHandler = defaultMediaItemClickHandler(LocalNavigator.current),
    cardHeight: Dp = 150.dp,
    shape: Shape = MaterialTheme.shapes.medium,
    enablePurchaseOptionsOverlay: Boolean = true
) {
    val displaySettings = media.requireDisplaySettings().withOverrides(displayOverrides)
    val (imageUrl, aspectRatio) = displaySettings.thumbnailUrlAndRatio ?: ("" to AspectRatio.SQUARE)
    if (media.isDisabled) {
        DisabledCard(imageUrl, media, shape, cardHeight, aspectRatio, modifier)
    } else {
        val showPurchaseOptions = enablePurchaseOptionsOverlay && (media.showPurchaseOptions || media.showAlternatePage)
        ImageCard(
            imageUrl = imageUrl,
            contentDescription = media.nameOrLockedName(),
            shape = shape,
            focusedOverlay = {
                val padding = if (aspectRatio == AspectRatio.WIDE) 18.dp else 12.dp
                Column(Modifier.padding(padding)) {
                    if (showPurchaseOptions) {
                        Text(
                            text = "View purchase options".uppercase(),
                            style = MaterialTheme.typography.button_24,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    MetadataTexts(displaySettings)
                }
            },
            unFocusedOverlay = {
                if (media.mediaType == MediaEntity.MEDIA_TYPE_VIDEO) {
                    val liveVideoInfo = media.liveVideoInfo
                    if (liveVideoInfo != null) {
                        LiveVideoUnFocusedOverlay(liveVideoInfo)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center)
                                .alpha(0.75f)
                        )
                    }
                }
                if (showPurchaseOptions) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black(alpha = 0.8f))
                    )
                }
            },
            onClick = { onMediaItemClick(media, permissionContext) },
            modifier = modifier
                .height(cardHeight)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
        )
    }
}

@Composable
private fun DisabledCard(
    imageUrl: String,
    media: MediaEntity,
    shape: Shape,
    cardHeight: Dp,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .alpha(MaterialTheme.colorScheme.disabledItemAlpha)
            .clip(shape)
    ) {
        ShimmerImage(
            imageUrl,
            contentDescription = media.nameOrLockedName(),
            modifier = modifier
                .height(cardHeight)
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
        )
        media.liveVideoInfo?.let {
            LiveVideoUnFocusedOverlay(it)
        }
    }
}

@Composable
private fun BoxScope.LiveVideoUnFocusedOverlay(liveVideo: LiveVideoInfoEntity) {
    when {
        liveVideo.ended -> {
            // Maybe never even displayed in the UI in the first place?
        }

        liveVideo.started -> {
            Text(
                "LIVE",
                style = MaterialTheme.typography.button_24.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .padding(15.dp)
                    .background(Color.Red, shape = RoundedCornerShape(2.dp))
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            )
        }

        else /* Upcoming */ -> {
            val startTime = liveVideo.getStartDateTimeString(LocalContext.current)
            Text(
                "UPCOMING\n$startTime",
                style = MaterialTheme.typography.button_24.copy(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                color = Color(0xFFB3B3B3),
                modifier = Modifier
                    .padding(15.dp)
                    .background(Color(0xFF272727), shape = RoundedCornerShape(2.dp))
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Navigates to the appropriate destination based on the media type.
 */
private fun defaultMediaItemClickHandler(navigator: Navigator): MediaClickHandler =
    { media, permissionContext ->
        media.onClickDirection(permissionContext)
            ?.let { navigator(it.asPush()) }
    }

@Preview(heightDp = 150, widthDp = 300)
@Composable
fun NonVideoCardPreview() = EluvioThemePreview {
    val media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        mediaType = MediaEntity.MEDIA_TYPE_IMAGE
    }
    Row(modifier = Modifier.padding(10.dp)) {
        MediaItemCard(media)
        Spacer(modifier = Modifier.width(10.dp))
        MediaItemCard(media, modifier = Modifier.requestInitialFocus())
    }
}

@Preview(heightDp = 330, widthDp = 270)
@Composable
fun VideoCardPreview() = EluvioThemePreview {
    val media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
        imageAspectRatio = AspectRatio.WIDE
    }
    Column(modifier = Modifier.padding(10.dp)) {
        MediaItemCard(media)
        Spacer(modifier = Modifier.height(10.dp))
        MediaItemCard(media, modifier = Modifier.requestInitialFocus())
    }
}

@Preview(heightDp = 330, widthDp = 270)
@Composable
fun LiveVideoCardPreview() = EluvioThemePreview {
    val media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        displaySettings = DisplaySettingsEntity().apply {
            title = name
            subtitle = "The Grand Arena"
            headers = realmListOf("8pm Central", "Stage D", "Lorem Ipsum", "Dolor Sit Amet")
        }
        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
        imageAspectRatio = AspectRatio.WIDE
        liveVideoInfo = LiveVideoInfoEntity().apply {
            startTime = RealmInstant.MIN
        }
    }
    Column(modifier = Modifier.padding(10.dp)) {
        MediaItemCard(media)
        Spacer(modifier = Modifier.height(10.dp))
        MediaItemCard(media, modifier = Modifier.requestInitialFocus())
    }
}

@Preview(heightDp = 330, widthDp = 270)
@Composable
fun UpcomingLiveVideoCardPreview() = EluvioThemePreview {
    val media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        displaySettings = DisplaySettingsEntity().apply {
            title = name
            subtitle = "The Grand Arena"
            headers = realmListOf("8pm Central", "Stage D", "Lorem Ipsum", "Dolor Sit Amet")
        }
        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
        imageAspectRatio = AspectRatio.WIDE
        liveVideoInfo = LiveVideoInfoEntity().apply {
            startTime = RealmInstant.MAX
        }
    }
    Column(modifier = Modifier.padding(10.dp)) {
        MediaItemCard(media)
        Spacer(modifier = Modifier.height(10.dp))
        MediaItemCard(media, modifier = Modifier.requestInitialFocus())
    }
}

@Preview(heightDp = 150, widthDp = 270, locale = "fr")
@Composable
fun EndedLiveVideoCardPreview() = EluvioThemePreview {
    MediaItemCard(media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        mediaType = MediaEntity.MEDIA_TYPE_VIDEO
        imageAspectRatio = AspectRatio.WIDE
        liveVideoInfo = LiveVideoInfoEntity().apply {
            endTime = RealmInstant.MIN
        }
    })
    Text("UNDEFINED DESIGN: TBD", modifier = Modifier.align(Alignment.Center))
}

@Preview(heightDp = 150, widthDp = 270, locale = "fr")
@Composable
fun DisabledCardPreview() = EluvioThemePreview {
    MediaItemCard(media = MediaEntity().apply {
        id = "id"
        name = "NFT Media Item"
        displaySettings = DisplaySettingsEntity().apply {
            title = name
            subtitle = "The Grand Arena"
            headers = realmListOf("8pm Central", "Stage D", "Lorem Ipsum", "Dolor Sit Amet")
        }
        imageAspectRatio = AspectRatio.WIDE
        resolvedPermissions = VolatilePermissionSettings(
            authorized = false,
            behavior = PermissionBehavior.DISABLE.value,
            permissionItemIds = emptyList(),
            alternatePageId = null,
            secondaryMarketPurchaseOption = null
        )
        liveVideoInfo = LiveVideoInfoEntity().apply {
            startTime = RealmInstant.MIN
        }
    })
}
