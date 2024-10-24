package app.eluvio.wallet.data.entities.v2.search

import app.eluvio.wallet.data.entities.FabricUrlEntity
import io.realm.kotlin.types.EmbeddedRealmObject

class FilterValueEntity : EmbeddedRealmObject {
    companion object {
        const val ALL = "All"
        fun from(value: String) = FilterValueEntity().apply { this.value = value }
    }

    var value: String = ""
    var imageUrl: FabricUrlEntity? = null

    override fun toString(): String {
        return "Value(value='$value', imageUrl=$imageUrl)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterValueEntity

        if (value != other.value) return false
        if (imageUrl != other.imageUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        return result
    }
}
