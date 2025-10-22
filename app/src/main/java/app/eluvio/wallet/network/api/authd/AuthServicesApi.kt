package app.eluvio.wallet.network.api.authd

import app.eluvio.wallet.util.Device
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
    @POST("wlt/refresh/csat")
    fun refreshCsat(@Body body: RefreshCsatRequest): Single<CsatResponse>

    @POST("wlt/login/redirect/metamask")
    fun generateActivationCode(@Body body: ActivationCodeRequest): Single<ActivationCodeResponse>

    @GET("wlt/login/redirect/metamask/{code}/{passcode}")
    fun checkToken(
        @Path("code") code: String,
        @Path("passcode") passcode: String,
    ): Single<Response<CheckTokenResponse>>
}

interface DeepLinkAuthApi : AuthdApi {
    // Get a jwt from an "idToken"
    @POST("wlt/login/jwt")
    fun authdLogin(
        // Static body. no need to create special classes for it.
        @Body body: RequestBody = """{"ext":{"share_email":true}}"""
            .toRequestBody("application/json".toMediaType())
    ): Single<AuthTokenResponse>

    // Get a csat from a cluster token
    fun csat(@Body body: CsatRequestBody): Single<CsatResponse>
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
data class CsatRequestBody(
    @field:Json(name = "tid") val tenantId: String?,
    /** A unique device identifier. */
    @field:Json(name = "nonce") val nonce: String,
    val email: String? = null,
    val force: Boolean = true,
    /**
     * How long in seconds the resulting token should be valid for. This is for testing only, since
     * the default is the max value, so adding this parameter can only make your session shorter.
     */
    val exp: Long? = null,

    val app_name: String = Device.NAME,
)

@JsonClass(generateAdapter = true)
data class CsatResponse(
    @field:Json(name = "token") val fabricToken: String,
    @Deprecated("Use [address] instead") @field:Json(name = "addr") val addr: String?,
    @Deprecated("Use [address] instead") @field:Json(name = "user_addr") val userAddress: String?,
    @field:Json(name = "refresh_token") val refreshToken: String?,
    val clusterToken: String?,
    val expiresAt: Long?,
    val email: String?,
) {
    // Server is inconsistent with naming of this field.
    val address = addr ?: userAddress
}

@JsonClass(generateAdapter = true)
data class RefreshCsatRequest(
    @field:Json(name = "refresh_token") val refreshToken: String,
    val nonce: String,
    @field:Json(name = "last_csat") val currentFabricToken: String,
    /**
     * How long in seconds the resulting token should be valid for. This is for testing only, since
     * the default is the max value, so adding this parameter can only make your session shorter.
     */
    val exp: Long? = null,
)

@JsonClass(generateAdapter = true)
data class ActivationCodeRequest(
    @field:Json(name = "dest") val destination: String,
    val op: String = "create",
)

@JsonClass(generateAdapter = true)
data class ActivationCodeResponse(
    @field:Json(name = "id") val code: String,
    @field:Json(name = "passcode") val passcode: String,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "expiration") val expiration: Long,
)

@JsonClass(generateAdapter = true)
data class CheckTokenResponse(
    // [payload] holds a string, but it can be parsed into [CsatResponse]
    val payload: String,
    // Refresh token is returned in the top level and needs to be set on the object in [payload]
    // to make a complete CsatResponse
    @field:Json(name = "refresh_token") val refreshToken: String?,
)
