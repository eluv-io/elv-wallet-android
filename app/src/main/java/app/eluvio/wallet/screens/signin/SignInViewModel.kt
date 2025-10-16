package app.eluvio.wallet.screens.signin

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.unsaved
import com.ramcosta.composedestinations.generated.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Authentication is extremely similar between all providers, so this class does the heavy lifting
 * and only delegates the provider-specific logic to the subclasses.
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    // Just for convenience, we always provide this, even if we don't use it (propertyId=null)
    private val propertyStore: MediaPropertyStore,
    private val tokenStore: TokenStore,
    private val urlShortener: UrlShortener,
    private val deviceActivationStore: DeviceActivationStore,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<SignInViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val propertyId: String = "",
        val loading: Boolean = true,
        val qrCode: Bitmap? = null,
        val userCode: String? = null,
        val bgImageUrl: FabricUrl? = null,
        val logoUrl: FabricUrl? = null,
    )

    private val navArgs = savedStateHandle.navArgs<SignInNavArgs>()

    // The bg image / logo will be fetched from the property, if available
    private val propertyId = navArgs.propertyId

    private var activationDataDisposable: Disposable? = null
    private var activationCompleteDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        updateState { copy(propertyId = propertyId) }
        observeActivationData()

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

    fun requestNewToken() {
        observeActivationData()
    }

    private fun observeActivationData() {
        activationDataDisposable?.dispose()
        activationDataDisposable = deviceActivationStore
            .observeActivationData(propertyId, navArgs.provider)
            .doOnNext {
                observeActivationComplete(it)
            }
            .switchMapSingle { activationData ->
                val url = activationData.url
                urlShortener.shorten(url)
                    .onErrorReturnItem(url)
                    .flatMap { generateQrCode(it) }
                    .map { qr -> activationData to qr }
            }
            .subscribeBy { (activationData, qrCode) ->
                updateState {
                    copy(
                        qrCode = qrCode,
                        userCode = activationData.code,
                        loading = false
                    )
                }
            }
            .addTo(disposables)
    }

    private fun observeActivationComplete(activationData: ActivationCodeResponse) {
        activationCompleteDisposable?.dispose()
        activationCompleteDisposable =
            Flowables.interval(5.seconds)
                .doOnSubscribe {
                    Log.d("starting to poll token for code=${activationData.code}")
                }
                .flatMapMaybe { deviceActivationStore.checkToken(activationData) }
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
                .doOnSuccess { token ->
                    Log.d("Got a token $token")
                    refreshAllPropertiesAsync()
                }
                .flatMapCompletable {
                    // Make sure property is actually fetched before we continue
                    prefetchPropertyAndSections()
                }
                .subscribeBy(
                    onComplete = {
                        Log.d("Activation complete and properties re-fetched, finishing auth flow.")
                        tokenStore.update(tokenStore.loginProvider to navArgs.provider)

                        navArgs.onSignedInDirection?.let { navigateTo(it.asReplace()) }
                    }
                )
                .addTo(disposables)
    }

    // Kick off a bg fetch of all properties.
    // This is a slower operation so we're not going to wait for it,
    // and we'll allow it to outlive the ViewModel scope.
    private fun refreshAllPropertiesAsync() {
        propertyStore.fetchMediaProperties()
            .subscribeBy(
                onError = { error ->
                    Log.d("Error fetching properties post-auth. Non critical.", error)
                }
            )
            .unsaved()
    }

    private fun prefetchPropertyAndSections(): Completable {
        return propertyStore.fetchMediaProperty(propertyId)
            // Now we can look up the Property in the cache and be sure it's not the one from before auth was complete.
            .andThen(propertyStore.observeMediaProperty(propertyId, false))
            .firstElement()
            .flatMapSingle { property ->
                property
                    .getFirstAuthorizedPage(currentPage = null, propertyStore)
                    .map { page -> property to page }
            }
            .flatMapCompletable { (property, page) ->
                propertyStore.observeSections(property, page)
                    // Assume that page Sections will never be zero
                    .takeUntil { it.isNotEmpty() }
                    .ignoreElements()
            }
    }
}
