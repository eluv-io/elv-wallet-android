package app.eluvio.wallet.network.dto

import app.eluvio.wallet.network.api.authd.RedeemableOffersApi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InitiateRedemptionRequest(
    // random uuid.base58 e.g. "FooZeuHkupYb6cKKxDRAyQ",
    @field:Json(name = "client_reference_id") val clientReferenceId: String,
    @field:Json(name = "tok_addr") val contractAddress: String,
    @field:Json(name = "tok_id") val tokenId: String,
    @field:Json(name = "offer_id") val offerId: Int,
    val op: String = RedeemableOffersApi.REDEEM_OPERATION,
)

@JsonClass(generateAdapter = true)
data class RedemptionStatusDto(
    // A composite field of the form: op:contract:tokenId:offerId:clientRef
    @field:Json(name = "op") val operationKey: String,
    val status: String?,
) {
    val operation: String?
    val contract: String?
    val tokenId: String?
    val offerId: String?
    val clientRef: String?

    init {
        val split = operationKey.split(":")
        this.operation = split.getOrNull(0)
        this.contract = split.getOrNull(1)
        this.tokenId = split.getOrNull(2)
        this.offerId = split.getOrNull(3)
        this.clientRef = split.getOrNull(4)
    }
}
