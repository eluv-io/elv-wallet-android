package app.eluvio.wallet.util

import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.data.entities.v2.PropertyLoginInfoRealmEntity
import app.eluvio.wallet.network.api.Auth0Api
import retrofit2.Retrofit
import retrofit2.create

object Auth0Util {
    fun getApiInfo(
        loginInfo: PropertyLoginInfoRealmEntity?,
        auth0Retrofit: Retrofit,
        auth0Api: Auth0Api
    ): Triple<Auth0Api, String, String> {
        val domain = loginInfo?.auth0Domain
        val clientId = loginInfo?.auth0ClientId
        return getApiInfo(domain, clientId, auth0Retrofit, auth0Api)
    }

    fun getApiInfo(
        domain: String?,
        clientId: String?,
        auth0Retrofit: Retrofit,
        auth0Api: Auth0Api
    ): Triple<Auth0Api, String, String> {
        if (domain != null && clientId != null) {
            val api = auth0Retrofit.newBuilder()
                .baseUrl(domain)
                .build()
                .create<Auth0Api>()
            return Triple(api, clientId, domain)
        } else {
            return Triple(
                auth0Api,
                BuildConfig.AUTH0_CLIENT_ID,
                auth0Retrofit.baseUrl().toString()
            )
        }
    }
}
