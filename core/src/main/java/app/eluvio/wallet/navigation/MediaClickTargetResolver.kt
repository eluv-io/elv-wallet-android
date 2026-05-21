package app.eluvio.wallet.navigation

import app.eluvio.wallet.core.BuildConfig
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionBehavior
import app.eluvio.wallet.data.entities.v2.permissions.behaviorEnum
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.util.logging.Log

/**
 * Figures out where we should go when a media item is clicked.
 */
fun MediaEntity.onClickTarget(permissionContext: PermissionContext?): NavTarget? {
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
): NavTarget? {
    return when {
        BuildConfig.DISABLE_PURCHASE_PROMPTS && (media.showAlternatePage || media.showPurchaseOptions) -> null

        media.showAlternatePage -> {
            NavTarget.PurchasePrompt(permissionContext, pageOverride = media.resolvedPermissions?.alternatePageId)
        }

        media.showPurchaseOptions -> {
            NavTarget.PurchasePrompt(permissionContext)
        }

        media.isUnauthorizedWithUnknownBehavior -> {
            Log.e("Tried to open unauthorized media, but behavior is unsupported or undefined: $media")
            null
        }

        media.mediaItemsIds.isNotEmpty() -> {
            // This media item is a container for other media (e.g. a media list/collection)
            NavTarget.MediaGrid(permissionContext)
        }

        media.liveVideoInfo?.streamStarted == false -> {
            // this is a live video that hasn't started yet.
            NavTarget.UpcomingVideo(
                propertyId = permissionContext.propertyId,
                mediaItemId = media.id,
                sourcePageId = permissionContext.pageId,
            )
        }

        media.mediaType in listOf(
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO,
        ) -> NavTarget.VideoPlayer(
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
private fun clickWithoutContext(media: MediaEntity): NavTarget? {
    if (media.requireLockedState().locked) {
        // This is media_wallet_v1 concept of "locked". Deprecated in media_wallet_v2.
        return NavTarget.LockedMediaDialog(
            media.nameOrLockedName(),
            media.imageOrLockedImage(),
            media.requireLockedState().subtitle,
            media.aspectRatio(),
        )
    } else {
        return when (media.mediaType) {
            MediaEntity.MEDIA_TYPE_LIVE_VIDEO,
            MediaEntity.MEDIA_TYPE_VIDEO -> NavTarget.VideoPlayer(media.id)

            MediaEntity.MEDIA_TYPE_IMAGE,
            MediaEntity.MEDIA_TYPE_GALLERY -> NavTarget.ImageGallery(media.id)

            else -> if (media.mediaFile.isNotEmpty() || media.mediaLinks.isNotEmpty()) {
                NavTarget.ExternalMediaQrDialog(media.id)
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
