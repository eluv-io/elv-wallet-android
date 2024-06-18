package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.network.dto.v2.MediaPageSectionDto
import app.eluvio.wallet.network.dto.v2.SectionItemDto
import io.realm.kotlin.ext.toRealmList

private val supportedSectionTypes = setOf("media")

fun MediaPageSectionDto.toEntity(baseUrl: String): MediaPageSectionEntity {
    val dto = this

    return MediaPageSectionEntity().apply {
        id = dto.id
        items = dto.content.mapNotNull { it.toEntity(baseUrl) }.toRealmList()
        title = dto.display?.title
        subtitle = dto.display?.subtitle
    }
}

private fun SectionItemDto.toEntity(baseUrl: String): MediaPageSectionEntity.SectionItemEntity? {
    val dto = this
    if (dto.type !in supportedSectionTypes) return null
    return MediaPageSectionEntity.SectionItemEntity().apply {
        mediaType = dto.mediaType
        media = dto.media?.toEntity(baseUrl)
        expand = dto.expand == true
    }
}