package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.network.dto.FulfillmentResponseDto
import app.eluvio.wallet.network.dto.InitiateRedemptionRequest
import app.eluvio.wallet.network.dto.NftInfoDto
import app.eluvio.wallet.network.dto.RedemptionStatusDto
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RedeemableOffersApi : AuthdApi {
    companion object {
        const val REDEEM_OPERATION = "nft-offer-redeem"
    }

    /**
     * Get the redeemable offers for a given NFT template
     */
    @GET("nft/info/{contractAddress}/{tokenId}")
    fun getNftInfo(
        @Path("contractAddress") contractAddress: String,
        @Path("tokenId") tokenId: String
    ): Single<NftInfoDto>

    @POST("wlt/act/{tenant}")
    fun initiateRedemption(
        @Path("tenant") tenant: String,
        @Body body: InitiateRedemptionRequest,
        // Doesn't actually redeem the offer. The BE will return "redeeming" for 10 seconds,
        // then "redeemed" for 2min.
        @Query("dry_run") dryRun: Boolean = false,
    ): Completable

    /**
     * Get the fulfillment data for a given transaction hash of a an offer that has already been fulfilled.
     */
    @GET("https://appsvc.svc.eluv.io/code-fulfillment/{network}/fulfill/{tx_hash}")
    fun getFulfillmentData(
        @Path("network") networkName: String,
        @Path("tx_hash") transactionHash: String
    ): Single<FulfillmentResponseDto>

    @GET("wlt/status/act/{tenant}")
    fun getRedemptionStatus(@Path("tenant") tenant: String): Single<List<RedemptionStatusDto>>
}
