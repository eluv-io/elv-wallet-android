package app.eluvio.wallet.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PagedContent<Content>(
    val contents: List<Content>?,
    val paging: PagingInfo
)

@JsonClass(generateAdapter = true)
data class PagingInfo(
    val start: Int,
    val limit: Int,
    val total: Int,
)
