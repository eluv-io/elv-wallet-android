package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.network.dto.ContractInfoDto
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface ContractInfoApi : AuthdApi {
    @GET("nft/info/{contractAddress}")
    fun getContractInfo(@Path("contractAddress") contractAddress: String): Single<ContractInfoDto>
}
