package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.PropertyLoginInfoRealmEntity
import app.eluvio.wallet.di.Auth0
import app.eluvio.wallet.network.api.Auth0Api
import app.eluvio.wallet.network.api.Auth0Request
import app.eluvio.wallet.network.api.DeviceActivationData
import app.eluvio.wallet.network.api.GetTokenRequest
import app.eluvio.wallet.network.api.GetTokenResponse
import app.eluvio.wallet.util.Auth0Util
import app.eluvio.wallet.util.logging.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DeviceActivationStore @Inject constructor(
    @Auth0 private val auth0Retrofit: Retrofit,
    private val auth0Api: Auth0Api,
    private val tokenStore: TokenStore,
) {
    fun observeActivationData(
        loginInfo: PropertyLoginInfoRealmEntity?,
    ): Observable<DeviceActivationData> {
        val (api, clientId) = Auth0Util.getApiInfo(loginInfo, auth0Retrofit, auth0Api)
        return api.getAuth0ActivationData(Auth0Request(clientId = clientId))
            .doOnSuccess {
                Log.d("Auth0 activation data fetched: $it")
            }
            .toObservable()
            // Make observable never-ending so we can restart it even after getting successful result from auth0
            .mergeWith(Observable.never())
            .map {
                // [it.verificationUri] is correct, but not pretty. Hardcoding short link
                it.copy(verificationUri = "https://elv.lv/activate")
            }
            .timeout {
                Observable.timer(it.expiresInSeconds, TimeUnit.SECONDS)
                    .doOnComplete {
                        Log.d("ActivationData timeout reached, re-fetching from auth0")
                    }
            }
            .retry()
    }

    fun checkToken(
        deviceCode: String,
        loginInfo: PropertyLoginInfoRealmEntity?
    ): Single<Response<GetTokenResponse>> {
        val (api, clientId, domain) = Auth0Util.getApiInfo(loginInfo, auth0Retrofit, auth0Api)
        return api.getToken(GetTokenRequest(clientId = clientId, deviceCode = deviceCode))
            .doOnSuccess {
                Log.d("check token result $it")
                val response = it.body()
                tokenStore.update(
                    tokenStore.idToken to response?.idToken,
                    tokenStore.accessToken to response?.accessToken,
                    tokenStore.refreshToken to response?.refreshToken,
                    tokenStore.auth0Domain to domain,
                    tokenStore.auth0ClientId to clientId,

                    // Clear out previous tokens at this stage, as we are ready to get new ones
                    tokenStore.fabricToken to null,
                    tokenStore.walletAddress to null,
                )
            }
    }
}
