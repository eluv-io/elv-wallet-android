package app.eluvio.wallet.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FulfillmentResponseDto(
    val message: String?,
    val fulfillment_data: FulfillmentDataDto?,
)

@JsonClass(generateAdapter = true)
data class FulfillmentDataDto(
    val url: String?,
    val code: String,
)
