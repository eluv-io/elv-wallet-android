package app.eluvio.wallet.navigation

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.core.BuildConfig
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionBehavior
import app.eluvio.wallet.data.entities.v2.permissions.behaviorEnum
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.screens.gallery.ImageGalleryNavArgs
import app.eluvio.wallet.screens.nftdetail.legacy.LockedMediaDialogNavArgs
import app.eluvio.wallet.screens.property.mediagrid.MediaGridNavArgs
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideoNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import app.eluvio.wallet.screens.qrdialogs.externalmedia.ExternalMediaQrDialogNavArgs
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import app.eluvio.wallet.util.logging.Log

/**
 * Figures out where we should go when a media item is clicked.
 */
fun MediaEntity.onClickTarget(permissionContext: PermissionContext?): NavKey? {
    return when (permissionContext) {
        null -> clickWithoutContext(this)
        else -> clickWithPermissionContext(this, permissionContext)
    }
        .also { result ->
            if (result != null) {
                Log.v("Clicked on Media, navigating to: $result")
            } else {
                Log.w("No target found for media: $this")
            }
        }
}

private fun clickWithPermissionContext(
    media: MediaEntity,
    permissionContext: PermissionContext
): NavKey? {
    return when {
        BuildConfig.DISABLE_PURCHASE_PROMPTS && (media.showAlternatePage || media.showPurchaseOptions) -> null

        media.showAlternatePage -> {
            PurchasePromptNavArgs(permissionContext, pageOverride = media.resolvedPermissions?.alternatePageId)
        }

        media.showPurchaseOptions -> {
            PurchasePromptNavArgs(permissionContext)
        }

        media.isUnauthorizedWithUnknownBehavior -> {
            Log.e("Tried to open unauthorized media, but behavior is unsupported or undefined: $media")
            null
        }

        media.mediaItemsIds.isNotEmpty() -> {
            // This media item is a container for other media (e.g. a media list/collection)
            MediaGridNavArgs(permissionContext)
        }

        media.liveVideoInfo?.streamStarted == false -> {
            // this is a live video that hasn't started yet.
            UpcomingVideoNavArgs(
                propertyId = permissionContext.propertyId,
                mediaItemId = media.id,
            )
        }

        media.mediaType in listOf(
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO,
        ) -> VideoPlayerArgs(
            mediaItemId = media.id,
            mediaTitle = media.requireDisplaySettings().title,
            propertyId = permissionContext.propertyId
        )

        else -> clickWithoutContext(media)
    }
}

/**
 * Handles click on media that has either been pre-checked for permissions, or comes from the legacy
 * world of NFTs without v2 permissions.
 */
private fun clickWithoutContext(media: MediaEntity): NavKey? {
    if (media.requireLockedState().locked) {
        // This is media_wallet_v1 concept of "locked". Deprecated in media_wallet_v2.
        return LockedMediaDialogNavArgs(
            media.nameOrLockedName(),
            media.imageOrLockedImage(),
            media.requireLockedState().subtitle,
            media.aspectRatio(),
        )
    } else {
        return when (media.mediaType) {
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO -> VideoPlayerArgs(media.id)

            MediaEntity.MEDIA_TYPE_IMAGE,
            MediaEntity.MEDIA_TYPE_GALLERY -> ImageGalleryNavArgs(media.id)

            else -> if (media.mediaFile.isNotEmpty() || media.mediaLinks.isNotEmpty()) {
                ExternalMediaQrDialogNavArgs(media.id)
            } else {
                Log.w("Tried to open unsupported media with no links: $media")
                null
            }
        }
    }
}

private val MediaEntity.isUnauthorizedWithUnknownBehavior: Boolean
    get() = resolvedPermissions?.authorized == false
            // Ignore show-if-unauthorized behavior,
            // because we need to treat that as if we do have access.
            && resolvedPermissions?.behaviorEnum != PermissionBehavior.ONLY_SHOW_IF_UNAUTHORIZED
