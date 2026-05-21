package app.eluvio.wallet.network.api

import app.eluvio.wallet.network.dto.FabricConfiguration
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Url

interface FabricConfigApi {
    @GET
    fun getConfig(@Url configUrl: String): Single<FabricConfiguration>
}
