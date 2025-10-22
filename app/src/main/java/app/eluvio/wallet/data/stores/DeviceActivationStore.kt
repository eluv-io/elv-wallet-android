package app.eluvio.wallet.data.stores

import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.ActivationCodeRequest
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.network.api.authd.AuthServicesApi
import app.eluvio.wallet.util.Device
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.sha512
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class DeviceActivationStore @Inject constructor(
    private val apiProvider: ApiProvider,
    private val tokenStore: TokenStore,
    private val environmentStore: EnvironmentStore,
    private val installation: Installation,
) {

    fun observeActivationData(
        propertyId: String,
        loginProvider: String
    ): Flowable<ActivationCodeResponse> {
        return apiProvider.getApi(AuthServicesApi::class)
            .zipWith(environmentStore.observeSelectedEnvironment().firstOrError())
            .flatMap { (api, env) ->
                val dest = buildString {
                    append(env.walletUrl)
                    append("?action=login&mode=login&response=code&source=code")
                    append("&pid=$propertyId")
                    append("&install_id=${installation.id.sha512}")
                    append("&origin=${Device.NAME}")
                    if (loginProvider != "ory") append("&clear=")
                    // append("&ttl=0.008") // For testing ~30sec token expiration
                    append("#/login")
                }
                api.generateActivationCode(ActivationCodeRequest(dest))
            }
            // Make observable never-ending so we can restart it even after getting successful result from auth0
            .mergeWith(Single.never())
            .timeout {
                val delay = (it.expiration * 1000 - Calendar.getInstance().timeInMillis)
                    .coerceIn(1.minutes.inWholeMilliseconds..7.minutes.inWholeMilliseconds)
                Flowable.timer(delay, TimeUnit.MILLISECONDS)
                    .doOnComplete {
                        Log.d("ActivationData timeout reached, re-fetching activation data")
                    }
            }
            .retry()
    }

    /**
     * Only returns a value when activation is complete and fabric token has been obtained.
     * Otherwise returns an empty [Maybe].
     */
    fun checkToken(activationData: ActivationCodeResponse): Maybe<String> {
        return apiProvider.getApi(AuthServicesApi::class)
            .flatMap { api -> api.checkToken(activationData.code, activationData.passcode) }
            .mapNotNull { httpResponse ->
                Log.d("check token result $httpResponse")
                val response = httpResponse.body() ?: return@mapNotNull null
                // Poll successful. Store login information.
                tokenStore.login(response.payload)
                // Return any non-null value to signal completion.
                return@mapNotNull response.payload.fabricToken
            }
    }
}
