package app.eluvio.wallet.screens.signin.common

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.NavGraphs
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.screens.navArgs
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.interval
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlin.time.Duration

/**
 * Authentication is extremely similar between all providers, so this class does the heavy lifting
 * and only delegates the provider-specific logic to the subclasses.
 */
abstract class BaseLoginViewModel<ActivationData : Any>(
    // Just for convenience, we always provide this, even if we don't use it (propertyId=null)
    private val propertyStore: MediaPropertyStore,
    private val tokenStore: TokenStore,
    private val urlShortener: UrlShortener,
    private val loginProvider: LoginProviders,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<LoginState>(LoginState(), savedStateHandle) {

    private val navArgs = savedStateHandle.navArgs<SignInNavArgs>()

    // The bg image / logo will be fetched from the property, if available
    protected val propertyId: String? = navArgs.propertyId

    abstract fun fetchActivationData(): Flowable<ActivationData>

    /**
     * Check if token has been activated and handle the result.
     * The [Maybe] should only emit a value when authentication is successful.
     */
    abstract fun ActivationData.checkToken(): Maybe<*>
    abstract fun ActivationData.getPollingInterval(): Duration
    abstract fun ActivationData.getQrUrl(): String
    abstract fun ActivationData.getCode(): String

    private var activationDataDisposable: Disposable? = null
    private var activationCompleteDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        observeActivationData()

        propertyId?.let { propertyId ->
            propertyStore.observeMediaProperty(propertyId, forceRefresh = false)
                .subscribeBy { property ->
                    updateState {
                        copy(
                            bgImageUrl = property.loginInfo?.backgroundImageUrl,
                            logoUrl = property.loginInfo?.logoUrl
                        )
                    }
                }
                .addTo(disposables)
        }
    }

    fun requestNewToken() {
        observeActivationData()
    }

    private fun observeActivationData() {
        activationDataDisposable?.dispose()
        activationDataDisposable = fetchActivationData()
            .doOnNext {
                observeActivationComplete(it)
            }
            .switchMapSingle { activationData ->
                val url = activationData.getQrUrl()
                urlShortener.shorten(url)
                    .onErrorReturnItem(url)
                    .flatMap { generateQrCode(it) }
                    .map { qr -> activationData to qr }
            }
            .subscribeBy { (activationData, qrCode) ->
                updateState {
                    copy(
                        qrCode = qrCode,
                        userCode = activationData.getCode(),
                        loading = false
                    )
                }
            }
            .addTo(disposables)
    }

    private fun observeActivationComplete(activationData: ActivationData) {
        activationCompleteDisposable?.dispose()
        activationCompleteDisposable =
            Flowables.interval(activationData.getPollingInterval())
                .doOnSubscribe {
                    Log.d("starting to poll token for code=${activationData.getCode()}")
                }
                .flatMapMaybe { activationData.checkToken() }
                .firstOrError(
                    // stop the interval as soon as [checkToken] returns a non-null value
                )
                .doOnError {
                    Log.e(
                        "Activation polling error! This shouldn't happen, restarting polling.",
                        it
                    )
                }
                .retry()
                .doOnSuccess { Log.d("Got a token $it") }
                .flatMapCompletable {
                    // Re-fetch properties because all the permissions will be different now that we have a token
                    propertyStore.fetchMediaProperties(clearOldProperties = true)
                }
                .subscribeBy(
                    onComplete = {
                        Log.d("Activation complete and properties re-fetched, finishing auth flow.")
                        tokenStore.loginProvider = loginProvider
                        navigateTo(NavigationEvent.PopTo(NavGraphs.authFlowGraph, true))
                        navArgs.onSignedInDirection?.let { navigateTo(it.asPush()) }
                    }
                )
                .addTo(disposables)
    }
}
