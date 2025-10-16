package app.eluvio.wallet.network.api.mwv2

import app.eluvio.wallet.network.api.authd.AuthdApi
import app.eluvio.wallet.network.dto.PagedContent
import app.eluvio.wallet.network.dto.v2.GetFiltersResponse
import app.eluvio.wallet.network.dto.v2.MediaPageSectionDto
import app.eluvio.wallet.network.dto.v2.SearchRequest
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi : AuthdApi {
    @GET("mw/properties/{propertyId}/filters")
    fun getSearchFilters(@Path("propertyId") propertyId: String): Single<GetFiltersResponse>

    @POST("mw/properties/{propertyId}/search")
    fun search(
        @Path("propertyId") propertyId: String,
        @Body request: SearchRequest,
        @Query("limit") limit: Int = 30,
    ): Single<PagedContent<MediaPageSectionDto>>
}
