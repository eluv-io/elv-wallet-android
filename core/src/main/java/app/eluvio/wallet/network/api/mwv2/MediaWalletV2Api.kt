package app.eluvio.wallet.network.api.mwv2

import app.eluvio.wallet.network.api.authd.AuthdApi
import app.eluvio.wallet.network.dto.PagedContent
import app.eluvio.wallet.network.dto.v2.MediaItemV2Dto
import app.eluvio.wallet.network.dto.v2.MediaPageDto
import app.eluvio.wallet.network.dto.v2.MediaPageSectionDto
import app.eluvio.wallet.network.dto.v2.MediaPropertyDto
import app.eluvio.wallet.network.dto.v2.permissions.GetPermissionResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MediaWalletV2Api : AuthdApi {
    /**
     * Get a list of all properties we have access to.
     */
    @GET("mw/properties")
    fun getProperties(@Query("include_public") includePublic: Boolean = true): Single<PagedContent<MediaPropertyDto>>

    @GET("mw/properties/{propertyId}/pages/{pageId}")
    fun getPage(
        @Path("propertyId") propertyId: String,
        @Path("pageId") pageId: String
    ): Single<MediaPageDto>

    /**
     * Request a list of sections by their IDs.
     */
    @POST("mw/properties/{propertyId}/sections?resolve_subsections=true")
    fun getSectionsById(
        @Path("propertyId") propertyId: String,
        @Body request: List<String>
    ): Single<PagedContent<MediaPageSectionDto>>

    @GET("mw/properties/{propertyId}")
    fun getProperty(@Path("propertyId") propertyId: String): Single<MediaPropertyDto>

    @POST("mw/properties/{propertyId}/media_items")
    fun getMediaItemsById(
        @Path("propertyId") propertyId: String,
        @Body request: List<String>
    ): Single<PagedContent<MediaItemV2Dto>>

    @GET("mw/properties/{propertyId}/permissions?no_cache=true")
    fun getPermissionStates(@Path("propertyId") propertyId: String): Single<GetPermissionResponse>

    @POST("mw/properties/{propertyId}/sidebar/live")
    fun getStreamSelections(@Path("propertyId") propertyId: String): Single<PagedContent<MediaItemV2Dto>>
}
