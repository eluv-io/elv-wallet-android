package app.eluvio.wallet.screens.signin.auth0

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.network.api.DeviceActivationData
import app.eluvio.wallet.screens.signin.common.BaseLoginViewModel
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.mapNotNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class Auth0SignInViewModel @Inject constructor(
    private val deviceActivationStore: DeviceActivationStore,
    private val authenticationService: AuthenticationService,
    private val propertyStore: MediaPropertyStore,
    tokenStore: TokenStore,
    urlShortener: UrlShortener,
    savedStateHandle: SavedStateHandle
) : BaseLoginViewModel<DeviceActivationData>(
    propertyStore,
    tokenStore,
    urlShortener,
    LoginProviders.AUTH0,
    savedStateHandle
) {

    private val loginInfo = propertyId?.let { propertyId ->
        propertyStore
            .observeMediaProperty(propertyId)
            .firstOrError()
            .map { Optional.of(it.loginInfo) }
            .cache()
    } ?: Single.just(Optional.empty())

    override fun fetchActivationData(): Flowable<DeviceActivationData> {
        return loginInfo.flatMapObservable {
            deviceActivationStore.observeActivationData(it.orDefault(null))
        }.toFlowable(BackpressureStrategy.BUFFER)
    }

    override fun DeviceActivationData.getPollingInterval(): Duration = intervalSeconds.seconds

    override fun DeviceActivationData.getQrUrl(): String = verificationUriComplete

    override fun DeviceActivationData.getCode(): String = userCode

    override fun DeviceActivationData.checkToken(): Maybe<*> =
        loginInfo.flatMap {
            deviceActivationStore.checkToken(deviceCode, it.orDefault(null))
        }
            .mapNotNull { it.body() }
            .flatMapSingle { authenticationService.getFabricToken() }
}
