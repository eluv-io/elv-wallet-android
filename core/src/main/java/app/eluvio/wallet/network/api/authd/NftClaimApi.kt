package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.network.dto.InitiateEntitlementClaimRequest
import app.eluvio.wallet.network.dto.InitiateNftClaimRequest
import app.eluvio.wallet.network.dto.NftClaimStatusDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NftClaimApi : AuthdApi {
    companion object {
        const val NFT_CLAIM_OPERATION = "nft-claim"
        const val ENTITLEMENT_CLAIM_OPERATION = "nft-claim-entitlement"
    }

    @POST("wlt/act/{tenant}")
    fun claimNft(
        @Path("tenant") tenant: String,
        @Body body: InitiateNftClaimRequest
    ): Single<NftClaimStatusDto>

    @POST("wlt/act/{tenant}")
    fun claimEntitlement(
        @Path("tenant") tenant: String,
        @Body body: InitiateEntitlementClaimRequest
    ): Single<NftClaimStatusDto>

    @GET("wlt/status/act/{tenant}")
    fun getClaimStatus(@Path("tenant") tenant: String): Single<List<NftClaimStatusDto>>
}
