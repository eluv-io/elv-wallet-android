package app.eluvio.wallet.data.entities.v2.search

import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList

/**
 * A filter attribute that is very similar to the corresponding DTO class, but also hard to work with because it
 * doesn't include any [FilterOptions] data. Don't use directly, use [PropertySearchFiltersEntity.buildPrimaryFilter]
 * to get an instance of [SearchFilter] instead.
 */
class FilterAttributeEntity : EmbeddedRealmObject {
    var id: String = ""
    var title: String = ""
    var values: RealmList<FilterValueEntity> = realmListOf()

    override fun toString(): String {
        return "FilterAttributeEntity(id='$id', title='$title', values=$values)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterAttributeEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }
}
