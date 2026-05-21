package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.network.dto.VideoOptionsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface VideoPlayoutApi : AuthdApi {
    // Returns the first offering. To choose an offering use mw/playout_offering/{hash}
    @GET("mw/playout_options/{hash}")
    fun getVideoOptions(@Path("hash") hash: String): Single<VideoOptionsDto>

    @GET("mw/properties/{propertyId}/media_items/{mediaItemId}/offerings/any/playout_options")
    fun getVideoOptions(
        @Path("propertyId") propertyId: String,
        @Path("mediaItemId") mediaItemId: String
    ): Single<VideoOptionsDto>
}
