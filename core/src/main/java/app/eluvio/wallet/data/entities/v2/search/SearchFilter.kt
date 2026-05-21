package app.eluvio.wallet.data.entities.v2.search

import androidx.compose.runtime.Immutable
import app.eluvio.wallet.data.FabricUrl

/**
 * A fully built search filter that includes all [FilterOptions] data and references sub-filters objects,
 * rather than just ids.
 */
@Immutable
data class SearchFilter(
    val id: String,
    val title: String,
    val values: List<Value>,
    val style: Style
) {
    init {
        values.forEach { it.parent = this }
    }

    data class Value(
        val value: String,
        val nextFilter: SearchFilter? = null,
        val imageUrl: FabricUrl? = null
    ) {
        lateinit var parent: SearchFilter
    }

    enum class Style {
        IMAGE, TEXT;

        companion object {
            fun from(value: String?): Style {
                return when (value) {
                    "image" -> IMAGE
                    else -> TEXT
                }
            }
        }
    }
}
