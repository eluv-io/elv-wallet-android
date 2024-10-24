package app.eluvio.wallet.data.entities.v2.search

import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.FabricUrlEntity
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList

/**
 * Consolidates primary and secondary filter options into a single interface so we can more easily
 * build out [SearchFilter] in a generic way.
 */
interface FilterOptions {
    val value: String
    val image: FabricUrl?
    val nextFilterAttribute: String?
    val nextFilterOptions: List<FilterOptions>

    // Either "image" or "text" (assume "text" if null)
    val nextFilterStyle: String?
}

class PrimaryFilterOptionsEntity : EmbeddedRealmObject, FilterOptions {
    var primaryFilterValue: String = ""
    override val value: String get() = primaryFilterValue

    var secondaryFilterAttribute: String? = null
    override val nextFilterAttribute: String? get() = secondaryFilterAttribute

    var secondaryFilterOptions: RealmList<SecondaryFilterOptionsEntity> = realmListOf()
    override val nextFilterOptions: List<FilterOptions> get() = secondaryFilterOptions

    var secondaryFilterStyle: String? = null
    override val nextFilterStyle: String? get() = secondaryFilterStyle

    // No images for primary filters
    override val image: FabricUrl? get() = null

    override fun toString(): String {
        return "PrimaryFilterOptionsEntity(primaryFilterValue='$primaryFilterValue', secondaryFilterAttribute=$secondaryFilterAttribute, secondaryFilterOptions=$secondaryFilterOptions, secondaryFilterStyle=$secondaryFilterStyle)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrimaryFilterOptionsEntity

        if (primaryFilterValue != other.primaryFilterValue) return false
        if (secondaryFilterAttribute != other.secondaryFilterAttribute) return false
        if (secondaryFilterOptions != other.secondaryFilterOptions) return false
        if (secondaryFilterStyle != other.secondaryFilterStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primaryFilterValue.hashCode()
        result = 31 * result + (secondaryFilterAttribute?.hashCode() ?: 0)
        result = 31 * result + secondaryFilterOptions.hashCode()
        result = 31 * result + (secondaryFilterStyle?.hashCode() ?: 0)
        return result
    }
}

class SecondaryFilterOptionsEntity : EmbeddedRealmObject, FilterOptions {
    var secondaryFilterValue: String = ""
    override val value: String get() = secondaryFilterValue

    var secondaryFilterImage: FabricUrlEntity? = null
    override val image: FabricUrl? get() = secondaryFilterImage

    // No "next" filters for Secondary
    override val nextFilterAttribute: String? get() = null
    override val nextFilterOptions: List<FilterOptions> get() = emptyList()
    override val nextFilterStyle: String? get() = null

    override fun toString(): String {
        return "SecondaryFilterOptionsEntity(secondaryFilterValue='$secondaryFilterValue', secondaryFilterImage=$secondaryFilterImage)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecondaryFilterOptionsEntity

        if (secondaryFilterValue != other.secondaryFilterValue) return false
        if (secondaryFilterImage != other.secondaryFilterImage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = secondaryFilterValue.hashCode()
        result = 31 * result + (secondaryFilterImage?.hashCode() ?: 0)
        return result
    }
}

