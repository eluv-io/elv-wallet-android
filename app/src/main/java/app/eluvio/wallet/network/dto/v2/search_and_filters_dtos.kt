package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchRequest(
    @field:Json(name = "search_term")
    val searchTerm: String? = null,
    val tags: List<String>? = null,
    val attributes: Map<String, List<String>>? = null,
    // We don't actually send this to the server, but we need it for SearchRequests to fail equality checks
    // when subpropertyId changes.
    @field:Json(ignore = true)
    val subpropertyId: String? = null,
)

@JsonClass(generateAdapter = true)
data class GetFiltersResponse(
    // Property top-level tags and attributes.
    val tags: List<String>?,
    val attributes: Map<String, SearchFilterAttributeDto>?,

    // Anything with "filters" can define a primary and secondary filter.
    @field:Json(name = "primary_filter")
    val primaryFilter: String?,
    @field:Json(name = "secondary_filter")
    val secondaryFilter: String?,

    @field:Json(name = "filter_options")
    val filterOptions: List<FilterOptionsDto>?,
)

/**
 * This is a subset of [GetFiltersResponse], and we could techincally use the same class since everything is optional,
 * but I wanted to make sure we know that tags/attributes are not included in most "filters" fields.
 */
@JsonClass(generateAdapter = true)
data class SearchFiltersDto(
    @field:Json(name = "primary_filter")
    val primaryFilter: String?,
    @field:Json(name = "secondary_filter")
    val secondaryFilter: String?,

    @field:Json(name = "filter_options")
    val filterOptions: List<FilterOptionsDto>?,
)

@JsonClass(generateAdapter = true)
data class SearchFilterAttributeDto(
    val id: String,
    val title: String?,
    @field:Json(name = "tags")
    val values: List<String>?,
)

@JsonClass(generateAdapter = true)
data class FilterOptionsDto(
    @field:Json(name = "primary_filter_image")
    val image: AssetLinkDto?,
    @field:Json(name = "primary_filter_value")
    val primaryFilterValue: String,
    @field:Json(name = "secondary_filter_attribute")
    val secondaryFilterAttribute: String?
)
