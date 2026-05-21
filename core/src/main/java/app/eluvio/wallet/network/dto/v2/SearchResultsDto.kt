package app.eluvio.wallet.network.dto.v2

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultsDto(
    val tags: List<String>,
    val content: List<MediaPageSectionDto>,
)

@JsonClass(generateAdapter = true)
data class SearchGroupingDto(
    val title: String,
    val attributes: Map<String, String>,
    val tags: Map<String, String>,
    val media_type: Map<String, String>,
    val has_more: Boolean,
)
