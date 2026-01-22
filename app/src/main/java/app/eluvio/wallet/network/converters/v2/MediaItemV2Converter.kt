package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.AdditionalViewEntity
import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.entities.GalleryItemEntity
import app.eluvio.wallet.data.entities.LiveVideoInfoEntity
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import app.eluvio.wallet.data.entities.v2.search.FilterValueEntity
import app.eluvio.wallet.network.converters.toPathMap
import app.eluvio.wallet.network.dto.v2.AdditionalViewDto
import app.eluvio.wallet.network.dto.v2.DisplaySettingsDto
import app.eluvio.wallet.network.dto.v2.GalleryItemV2Dto
import app.eluvio.wallet.network.dto.v2.MediaItemV2Dto
import app.eluvio.wallet.util.realm.toRealmDictionaryOrEmpty
import app.eluvio.wallet.util.realm.toRealmInstant
import app.eluvio.wallet.util.realm.toRealmListOrEmpty

fun MediaItemV2Dto.toEntity(baseUrl: String): MediaEntity? {
    val dto = this
//    if (dto.mediaLink?.hashContainer?.get("resolution_error") != null) {
//        return null
//    }

    val display = (dto as DisplaySettingsDto).toEntity(baseUrl)

    val (imageFile, aspectRatio) = dto.mediaFile?.let { it to null }
        ?: dto.thumbnail_image_square?.let { it to AspectRatio.SQUARE }
        ?: dto.thumbnail_image_portrait?.let { it to AspectRatio.POSTER }
        ?: dto.thumbnail_image_landscape?.let { it to AspectRatio.WIDE }
        ?: (null to null)
    return MediaEntity().apply {
        id = dto.id
        name = dto.title ?: ""
        displaySettings = display
        mediaFile = imageFile?.path ?: ""
        imageAspectRatio = aspectRatio
        mediaType = dto.mediaType ?: dto.type
        image = display.thumbnailUrlAndRatio?.first ?: ""
        playableHash = dto.mediaLink?.hashContainer?.get("source")?.toString()
        mediaLinks = dto.mediaLink?.toPathMap().toRealmDictionaryOrEmpty()
        gallery = dto.gallery?.mapNotNull { it.toEntity() }.toRealmListOrEmpty()

        liveVideoInfo = parseLiveVideoInfo(dto)

        // Media Lists will have a list of media items under `media`, while Media Collections
        // will have a list of media lists under `mediaLists`. It is assumed that there will
        // only be one or the other, so we're just trying both with no real priority.
        mediaItemsIds = (dto.media ?: dto.mediaLists).toRealmListOrEmpty()

        attributes = dto.attributes?.map { (key, value) ->
            FilterAttributeEntity().apply {
                id = key
                values =
                    value.map { FilterValueEntity.from(it) }.toRealmListOrEmpty()
            }
        }.toRealmListOrEmpty()
        tags = dto.tags.toRealmListOrEmpty()
        additionalViews = dto.additionalViews?.mapNotNull { it.toEntity(baseUrl) }.toRealmListOrEmpty()
        rawPermissions = PermissionSettingsEntity().apply {
            permissionItemIds = dto.permissions.orEmpty()
                .takeIf {
                    // Server can still send a non-empty dto.permissions list even if the item is
                    // public. In that case we should ignore the list completely.
                    dto.public != true
                }
                ?.mapNotNull { it.permission_item_id }
                ?.plus(
                    // Add a dummy permission item that will always resolve to unauthorized. This
                    // won't affect Media Items that also have permissions defined, since it's
                    // enough to own one of the permissions to gain access. But we need it to make
                    // sure "private" media items are not accessible.
                    element = ""
                )
                .toRealmListOrEmpty()
        }
    }
}

private fun parseLiveVideoInfo(dto: MediaItemV2Dto): LiveVideoInfoEntity? {
    if (dto.liveVideo != true) {
        return null
    }
    return LiveVideoInfoEntity().apply {
        icons = dto.icons?.mapNotNull { it.icon?.path }.toRealmListOrEmpty()
        eventStartTime = dto.eventStartTime?.toRealmInstant()
        streamStartTime = dto.streamStartTime?.toRealmInstant()
        endTime = dto.endTime?.toRealmInstant()
    }
}

private fun GalleryItemV2Dto.toEntity(): GalleryItemEntity? {
    val dto = this
    return GalleryItemEntity().apply {
        // name = dto.title?
        // TODO: image should take precedence over thumbnail, if it exists
        imagePath = dto.thumbnail?.path ?: return null
        imageAspectRatio = AspectRatio.parse(dto.thumbnailAspectRatio)
    }
}

private fun AdditionalViewDto.toEntity(baseUrl: String): AdditionalViewEntity? {
    val dto = this
    val imagePath = dto.image?.path ?: return null
    return AdditionalViewEntity().apply {
        label = dto.label ?: ""
        imageUrl = FabricUrlEntity().apply { set(baseUrl, imagePath) }
        playableHash = dto.mediaLink?.hashContainer?.get("source")?.toString()
        mediaLinks = dto.mediaLink?.sources?.mapValues { it.value.path }.toRealmDictionaryOrEmpty()
    }
}
