package app.eluvio.wallet.network.converters

import app.eluvio.wallet.data.entities.NftId
import app.eluvio.wallet.data.entities.NftTemplateEntity
import app.eluvio.wallet.network.dto.NftTemplateDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty

/**
 * [primaryKey] should be generated by either [NftId.forToken] or [NftId.forSku].
 */
fun NftTemplateDto.toEntity(primaryKey: String): NftTemplateEntity {
    val dto = this
    return NftTemplateEntity().apply {
        id = primaryKey
        contractAddress = dto.address
        imageUrl = dto.image
        displayName = dto.display_name ?: ""
        editionName = dto.edition_name ?: ""
        description = dto.description ?: ""
        descriptionRichText = dto.description_rich_text
        // Currently, additional_media_sections is required. In the future we'll probably have
        // to support additional_media for backwards compatibility.
        dto.additional_media_sections?.let { additionalMediaSections ->
            featuredMedia =
                additionalMediaSections.featured_media?.map { it.toEntity(primaryKey) }
                    .toRealmListOrEmpty()
            mediaSections =
                additionalMediaSections.sections?.mapNotNull { it.toEntity(primaryKey) }
                    .toRealmListOrEmpty()
        }

    }
}