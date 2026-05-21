package app.eluvio.wallet.network.api.fabric

import app.eluvio.wallet.network.dto.VideoOptionsDto
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AssetFetcherApi : FabricApi {
    @GET("{path}")
    fun getVideoOptions(@Path("path") path: String): Single<Response<VideoOptionsDto>>
}
