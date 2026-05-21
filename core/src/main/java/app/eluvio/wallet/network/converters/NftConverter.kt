package app.eluvio.wallet.network.converters

import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.GalleryItemEntity
import app.eluvio.wallet.data.entities.MediaCollectionEntity
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.MediaSectionEntity
import app.eluvio.wallet.data.entities.NftEntity
import app.eluvio.wallet.data.entities.NftId
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.network.dto.GalleryItemDto
import app.eluvio.wallet.network.dto.MediaCollectionDto
import app.eluvio.wallet.network.dto.MediaItemDto
import app.eluvio.wallet.network.dto.MediaLinkDto
import app.eluvio.wallet.network.dto.MediaSectionDto
import app.eluvio.wallet.network.dto.NftDto
import app.eluvio.wallet.network.dto.RedeemableOfferDto
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.toRealmDictionaryOrEmpty
import app.eluvio.wallet.util.realm.toRealmInstant
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.types.RealmDictionary

fun List<NftDto>.toNfts(): List<NftEntity> {
    return mapNotNull { dto ->
        if (dto.nft_template.error != null) {
            Log.e("Fabric error. Probably Bad/expired Token.")
            return@mapNotNull null
        }
        // What makes this token truly unique is the combination of contract address and token id.
        // This is needed because the internal entities (sections, collections, media) have their own ID, but it's not actually unique.
        // Two MediaItems with the same ID can point to different assets, so we need to persist them differently.
        val tokenUniqueId = NftId.forToken(dto.contract_addr, dto.token_id)
        NftEntity().apply {
            id = tokenUniqueId
            createdAt = dto.created ?: 0
            nftTemplate = dto.nft_template.toEntity(tokenUniqueId)
            tokenId = dto.token_id
            // TODO: versionHash is currently parsed from token_uri, but in most cases it means it's
            //  an outdated hash. Once this is added to the API, we should just get it from there.
            versionHash = dto.token_uri?.split("/")?.firstOrNull { it.startsWith("hq__") }
            imageUrl = dto.meta.image ?: dto.nft_template.image ?: ""
            displayName = dto.meta.display_name ?: dto.nft_template.display_name ?: ""

            redeemableOffers =
                dto.nft_template.redeemable_offers?.map { it.toEntity() }
                    .toRealmListOrEmpty()
        }
    }
}

private fun RedeemableOfferDto.toEntity(): RedeemableOfferEntity {
    val dto = this
    return RedeemableOfferEntity().apply {
        offerId = dto.offer_id
        name = dto.name
        description = dto.description ?: ""
        imagePath = dto.image?.path
        posterImagePath = dto.poster_image?.path
        availableAt = dto.available_at?.toRealmInstant()
        expiresAt = dto.expires_at?.toRealmInstant()
        animation = dto.animation.toPathMap()
        redeemAnimation = dto.redeem_animation.toPathMap()

        dto.visibility?.let { visibility ->
            hide = visibility.hide == true
            hideIfExpired = visibility.hide_if_expired == true
            hideIfUnreleased = visibility.hide_if_unreleased == true
        }
    }
}

fun MediaSectionDto.toEntity(idPrefix: String): MediaSectionEntity? {
    val dto = this
    // Ignore sections with no collections
    dto.collections ?: return null
    return MediaSectionEntity().apply {
        id = "${idPrefix}_${dto.id}"
        name = dto.name ?: ""
        collections = dto.collections.map { it.toEntity(idPrefix) }.toRealmList()
    }
}

fun MediaCollectionDto.toEntity(idPrefix: String): MediaCollectionEntity {
    val dto = this
    return MediaCollectionEntity().apply {
        id = "${idPrefix}_${dto.id}"
        name = dto.name ?: ""
        display = dto.display ?: ""
        media = dto.media?.map { it.toEntity(idPrefix) }.toRealmListOrEmpty()
    }
}

fun MediaItemDto.toEntity(idPrefix: String): MediaEntity {
    val dto = this
    return MediaEntity().apply {
        id = "${idPrefix}_${dto.id}"
        name = dto.name ?: ""
        image = dto.image ?: ""
        posterImagePath = dto.poster_image?.path
        mediaType = dto.media_type ?: ""
        imageAspectRatio = AspectRatio.parse(dto.image_aspect_ratio)
        mediaFile = dto.media_file?.path ?: ""
        playableHash = dto.media_link?.hashContainer?.get("source")?.toString()
        mediaLinks = dto.media_link.toPathMap()
        tvBackgroundImage = dto.background_image_tv?.path ?: ""
        gallery = dto.gallery?.map { it.toEntity() }.toRealmListOrEmpty()
        lockedState = dto.getLockedState()
    }
}

private fun MediaItemDto.getLockedState(): MediaEntity.LockedStateEntity {
    val dto = this
    return MediaEntity.LockedStateEntity().apply {
//        locked = dto.locked == true
        locked = false // TODO: until we add new locked state API, pretend everything is unlocked
        hideWhenLocked = dto.locked_state?.hide_when_locked == true
        lockedImage = dto.locked_state?.image
        lockedName = dto.locked_state?.name
        imageAspectRatio = AspectRatio.parse(dto.locked_state?.image_aspect_ratio)
        subtitle = dto.locked_state?.subtitle_1
    }
}

fun GalleryItemDto.toEntity(): GalleryItemEntity {
    val dto = this
    return GalleryItemEntity().apply {
        name = dto.name
        imagePath = dto.image?.path
    }
}

fun MediaLinkDto?.toPathMap(): RealmDictionary<String> {
    return this?.sources
        ?.mapValues { (_, link) -> link.path }
        .toRealmDictionaryOrEmpty()
}
