package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.network.dto.NftDto
import app.eluvio.wallet.network.dto.NftForSkuResponse
import app.eluvio.wallet.network.dto.PagedContent
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GatewayApi : AuthdApi {
    @GET("apigw/nfts")
    fun getNfts(
        // 100 is a pretty big limit and will slow things down, but until we have proper
        // pagination support, it'll help us get around bugs in SKU/Entitlement flows.
        @Query("limit") limit: Int = 100
    ): Single<PagedContent<NftDto>>

    @GET("apigw/nfts")
    fun search(
        @Query("name_like") searchTerm: String? = null,
        @Query("property_id") propertyId: String? = null,
        @Query("limit") limit: Int = 100
    ): Single<PagedContent<NftDto>>

    @GET("apigw/marketplaces/{marketplaceId}/sku/{sku}")
    fun getNftForSku(
        @Path("marketplaceId") marketplaceId: String,
        @Path("sku") sku: String,
        @Query("signedEntitlementMessage") signedEntitlementMessage: String?
    ): Single<NftForSkuResponse>
}
