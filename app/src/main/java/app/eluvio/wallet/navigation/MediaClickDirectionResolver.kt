package app.eluvio.wallet.navigation

import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionBehavior
import app.eluvio.wallet.data.entities.v2.permissions.behaviorEnum
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.generated.destinations.ExternalMediaQrDialogDestination
import com.ramcosta.composedestinations.generated.destinations.ImageGalleryDestination
import com.ramcosta.composedestinations.generated.destinations.LockedMediaDialogDestination
import com.ramcosta.composedestinations.generated.destinations.MediaGridDestination
import com.ramcosta.composedestinations.generated.destinations.PurchasePromptDestination
import com.ramcosta.composedestinations.generated.destinations.UpcomingVideoDestination
import com.ramcosta.composedestinations.generated.destinations.VideoPlayerActivityDestination
import com.ramcosta.composedestinations.spec.Direction

/**
 * Figures out where we should go when a media item is clicked.
 */
fun MediaEntity.onClickDirection(permissionContext: PermissionContext?): Direction? {
    return when (permissionContext) {
        null -> clickWithoutContext(this)
        else -> clickWithPermissionContext(this, permissionContext)
    }
        .also { result ->
            if (result != null) {
                Log.v("Clicked on Media, navigating to: $result")
            } else {
                Log.w("No direction found for media: $this")
            }
        }
}

private fun clickWithPermissionContext(
    media: MediaEntity,
    permissionContext: PermissionContext
): Direction? {
    return when {
        BuildConfig.DISABLE_PURCHASE_PROMPTS && (media.showAlternatePage || media.showPurchaseOptions) -> null

        media.showAlternatePage -> {
            PurchasePromptDestination(permissionContext, pageOverride = media.resolvedPermissions?.alternatePageId)
        }

        media.showPurchaseOptions -> {
            PurchasePromptDestination(permissionContext)
        }

        media.isUnauthorizedWithUnknownBehavior -> {
            Log.e("Tried to open unauthorized media, but behavior is unsupported or undefined: $media")
            null
        }

        media.mediaItemsIds.isNotEmpty() -> {
            // This media item is a container for other media (e.g. a media list/collection)
            MediaGridDestination(permissionContext)
        }

        media.liveVideoInfo?.streamStarted == false -> {
            // this is a live video that hasn't started yet.
            UpcomingVideoDestination(
                propertyId = permissionContext.propertyId,
                mediaItemId = media.id,
                sourcePageId = permissionContext.pageId,
            )
        }

        media.mediaType in listOf(
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO,
        ) -> VideoPlayerActivityDestination(
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
private fun clickWithoutContext(media: MediaEntity): Direction? {
    if (media.requireLockedState().locked) {
        // This is media_wallet_v1 concept of "locked". Deprecated in media_wallet_v2.
        return LockedMediaDialogDestination(
            media.nameOrLockedName(),
            media.imageOrLockedImage(),
            media.requireLockedState().subtitle,
            media.aspectRatio(),
        )
    } else {
        return when (media.mediaType) {
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO -> VideoPlayerActivityDestination(media.id)

            MediaEntity.MEDIA_TYPE_IMAGE,
            MediaEntity.MEDIA_TYPE_GALLERY -> ImageGalleryDestination(media.id)

            else -> if (media.mediaFile.isNotEmpty() || media.mediaLinks.isNotEmpty()) {
                ExternalMediaQrDialogDestination(media.id)
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
