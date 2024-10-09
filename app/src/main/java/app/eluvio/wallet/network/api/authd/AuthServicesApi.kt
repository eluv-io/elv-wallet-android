package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.data.stores.Environment
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthServicesApi : AuthdApi {
    @POST("wlt/login/jwt")
    fun authdLogin(
        // Static body. no need to create special classes for it.
        @Body body: RequestBody = """{"ext":{"share_email":true}}"""
            .toRequestBody("application/json".toMediaType())
    ): Single<AuthTokenResponse>

    @POST("wlt/sign/eth/{accountId}")
    fun authdSign(
        @Path("accountId") accountId: String,
        @Body body: SignBody
    ): Single<SignResponse>

    @POST("wlt/login/redirect/metamask")
    fun generateMetamaskCode(@Body body: MetamaskCodeRequest): Single<MetamaskActivationData>

    @GET("wlt/login/redirect/metamask/{code}/{passcode}")
    fun getMetamaskToken(
        @Path("code") code: String,
        @Path("passcode") passcode: String,
    ): Single<Response<MetamaskTokenResponse>>
}

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    @field:Json(name = "addr") val address: String,
    // Custodial wallets will return a "cluster token", and there will be additional steps to
    // create a fabric token from it.
    // Metamask will return a "fabric token" directly.
    val token: String,
    val expiresAt: Long?,

    // Optional. Returned from Ory, but not Auth0 or Metamask
    val clusterToken: String?,
    val email: String?,
)

@JsonClass(generateAdapter = true)
data class SignBody(val hash: String)

@JsonClass(generateAdapter = true)
data class SignResponse(
    @field:Json(name = "sig") val signature: String,
)

@JsonClass(generateAdapter = true)
data class MetamaskCodeRequest(
    @field:Json(name = "dest") val destination: String,
    val op: String = "create",
) {
    companion object {
        fun from(environment: Environment) =
            MetamaskCodeRequest(destination = "${environment.walletUrl}?action=login&mode=login&response=code&source=code#/login")
    }
}

@JsonClass(generateAdapter = true)
data class MetamaskActivationData(
    @field:Json(name = "id") val code: String,
    @field:Json(name = "passcode") val passcode: String,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "metamask_url") val metamaskUrl: String,
    @field:Json(name = "expiration") val expiration: Long,
)

@JsonClass(generateAdapter = true)
data class MetamaskTokenResponse(
    // [payload] holds a string, but it can be parsed into [AuthTokenResponse]
    val payload: String,
)
