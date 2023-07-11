package app.eluvio.wallet.network.api

import app.eluvio.wallet.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface Auth0Api {
    @POST("oauth/device/code")
    fun getAuth0ActivationData(
        @Body request: Auth0Request = Auth0Request()
    ): Single<DeviceActivationData>

    @POST("oauth/token")
    fun getToken(@Body request: GetTokenRequest): Single<Response<GetTokenResponse>>
}

@JsonClass(generateAdapter = true)
data class Auth0Request(
    @field:Json(name = "client_id") val clientId: String = BuildConfig.AUTH0_CLIENT_ID,
    @field:Json(name = "scope") val scope: String = "openid profile email",
)

@JsonClass(generateAdapter = true)
data class DeviceActivationData(
    @field:Json(name = "device_code") val deviceCode: String,
    @field:Json(name = "user_code") val userCode: String,
    @field:Json(name = "verification_uri") val verificationUri: String,
    @field:Json(name = "expires_in") val expiresInSeconds: Long,
    @field:Json(name = "verification_uri_complete") val verificationUriComplete: String,
    @field:Json(name = "interval") val intervalSeconds: Long,
)

@JsonClass(generateAdapter = true)
data class GetTokenRequest(
    @field:Json(name = "grant_type") val grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    @field:Json(name = "client_id") val clientId: String = BuildConfig.AUTH0_CLIENT_ID,
    @field:Json(name = "device_code") val deviceCode: String,
)

@JsonClass(generateAdapter = true)
data class GetTokenResponse(
    @field:Json(name = "id_token") val idToken: String,
)