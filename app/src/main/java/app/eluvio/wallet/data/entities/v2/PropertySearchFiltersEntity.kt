package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import app.eluvio.wallet.data.entities.v2.search.FilterOptions
import app.eluvio.wallet.data.entities.v2.search.FilterValueEntity
import app.eluvio.wallet.data.entities.v2.search.PrimaryFilterOptionsEntity
import app.eluvio.wallet.data.entities.v2.search.SearchFilter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class PropertySearchFiltersEntity : RealmObject {
    @PrimaryKey
    var propertyId: String = ""

    var tags: RealmList<String> = realmListOf()
    var attributes = realmDictionaryOf<FilterAttributeEntity?>()

    /** Do  not use directly. Use [buildPrimaryFilter] instead. */
    var primaryFilterAttribute: String? = null

    /** Do  not use directly. Use [buildPrimaryFilter] instead. */
    var secondaryFilterAttribute: String? = null

    /** Do  not use directly. Use [buildPrimaryFilter] instead. */
    var filterOptions = realmListOf<PrimaryFilterOptionsEntity>()

    fun buildPrimaryFilter(): SearchFilter? {
        return attributes[primaryFilterAttribute]?.build(filterOptions, secondaryFilterAttribute, attributes)
    }

    private fun FilterAttributeEntity.build(
        filterOptions: List<FilterOptions>,
        // Only the top level has a "default". Everything else has to be explicitly set via FilterOptions.
        defaultNextFilterAttribute: String?,
        attributeMap: Map<String, FilterAttributeEntity?>,
        style: SearchFilter.Style = SearchFilter.Style.TEXT
    ): SearchFilter {

        val src = this
        val values: List<SearchFilter.Value> = if (filterOptions.isEmpty()) {
            // Update nextFilter to global secondary filter
            val nextFilter = attributeMap[defaultNextFilterAttribute]?.build(
                filterOptions = emptyList(),
                defaultNextFilterAttribute = null,
                attributeMap = attributeMap
            )
            src.values.map {
                SearchFilter.Value(it.value, nextFilter, it.imageUrl)
            }
        } else {
            // Nuke any existing tags. If they don't exist in the filter options, they're don't matter.
            filterOptions.map { option ->
                SearchFilter.Value(
                    // Server sends empty string as an "all" filter options
                    value = option.value.ifEmpty { FilterValueEntity.ALL },
                    nextFilter = option.nextFilterAttribute
                        ?.let { attributeMap[it] }
                        ?.build(
                            filterOptions = option.nextFilterOptions,
                            defaultNextFilterAttribute = null,
                            attributeMap = attributeMap,
                            style = SearchFilter.Style.from(option.nextFilterStyle)
                        ),
                    imageUrl = option.image
                )
            }
        }

        return SearchFilter(
            id = src.id,
            title = src.title,
            values = values,
            style = style
        )
    }

    override fun toString(): String {
        return "PropertySearchFiltersEntity(propertyId='$propertyId', tags=$tags, attributes=$attributes, rawPrimaryFilter=$primaryFilterAttribute, rawSecondaryFilter=$secondaryFilterAttribute, rawFilterOptions=$filterOptions)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PropertySearchFiltersEntity

        if (propertyId != other.propertyId) return false
        if (tags != other.tags) return false
        if (attributes != other.attributes) return false
        if (primaryFilterAttribute != other.primaryFilterAttribute) return false
        if (secondaryFilterAttribute != other.secondaryFilterAttribute) return false
        if (filterOptions != other.filterOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = propertyId.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (primaryFilterAttribute?.hashCode() ?: 0)
        result = 31 * result + (secondaryFilterAttribute?.hashCode() ?: 0)
        result = 31 * result + filterOptions.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = PropertySearchFiltersEntity::class
    }
}
