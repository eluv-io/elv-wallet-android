package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import app.eluvio.wallet.data.entities.v2.search.FilterValueEntity
import app.eluvio.wallet.data.entities.v2.search.PrimaryFilterOptionsEntity
import app.eluvio.wallet.data.entities.v2.search.SecondaryFilterOptionsEntity
import app.eluvio.wallet.network.dto.v2.GetFiltersResponse
import app.eluvio.wallet.network.dto.v2.PrimaryFilterOptionsDto
import app.eluvio.wallet.network.dto.v2.SearchFilterAttributeDto
import app.eluvio.wallet.network.dto.v2.SecondaryFilterOptionsDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.realm.kotlin.ext.toRealmDictionary

fun GetFiltersResponse.toEntity(propId: String, baseUrl: String): PropertySearchFiltersEntity {
    val dto = this
    return PropertySearchFiltersEntity().apply {
        propertyId = propId

        tags = dto.tags.toRealmListOrEmpty()

        val attributeMap = dto.attributes.orEmpty()
            .mapValues { (_, attr) -> attr.toEntity() }
        attributes = attributeMap.toRealmDictionary()

        primaryFilterAttribute = dto.primaryFilter
        secondaryFilterAttribute = dto.secondaryFilter
        filterOptions = dto.filterOptions?.map { it.toEntity(baseUrl) }.toRealmListOrEmpty()
    }
}

private fun PrimaryFilterOptionsDto.toEntity(baseUrl: String): PrimaryFilterOptionsEntity {
    val dto = this
    return PrimaryFilterOptionsEntity().apply {
        primaryFilterValue = dto.primaryFilterValue
        secondaryFilterAttribute = dto.secondaryFilterAttribute
        secondaryFilterOptions = dto.secondaryFilterOptions?.map { it.toEntity(baseUrl) }.toRealmListOrEmpty()
        secondaryFilterStyle = dto.secondaryFilterStyle
    }
}

private fun SecondaryFilterOptionsDto.toEntity(baseUrl: String): SecondaryFilterOptionsEntity {
    val dto = this
    return SecondaryFilterOptionsEntity().apply {
        secondaryFilterValue = dto.value
        secondaryFilterImage = dto.image?.toUrl(baseUrl)
    }
}


private fun SearchFilterAttributeDto.toEntity(): FilterAttributeEntity {
    val dto = this
    return FilterAttributeEntity().apply {
        id = dto.id
        title = dto.title ?: ""
        values = dto.values
            ?.map { FilterValueEntity.from(it) }
            .toRealmListOrEmpty()
    }
}
