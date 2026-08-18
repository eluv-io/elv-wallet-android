package app.eluvio.wallet.network.dto.v2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The categorized rows that make up the Discover page, along with every Property they reference.
 */
@JsonClass(generateAdapter = true)
data class DiscoverDto(
    val contents: List<DiscoverRowDto>?,
    val properties: Map<String, MediaPropertyDto>?,
)

@JsonClass(generateAdapter = true)
data class DiscoverRowDto(
    /** Currently the only supported value is [TYPE_PROPERTIES]. */
    val type: String?,
    val title: String?,
    @field:Json(name = "property_ids")
    val propertyIds: List<String>?,
) {
    companion object {
        const val TYPE_PROPERTIES = "properties"
    }
}
