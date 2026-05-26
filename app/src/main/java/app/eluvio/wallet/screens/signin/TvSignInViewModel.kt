package app.eluvio.wallet.screens.signin

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.DeviceActivationFlow
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.util.logging.Log
import com.stavfx.nav3hiltvm.annotations.HiltNavKeyViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

/**
 * TV's device-activation sign-in screen. Renders the activation URL as a QR code; the
 * shared polling / token storage / property prefetch lives in [DeviceActivationFlow].
 */
@HiltNavKeyViewModel
open class TvSignInViewModel(
    @NavArg private val navArgs: SignInNavArgs,
    private val propertyStore: MediaPropertyStore,
    private val urlShortener: UrlShortener,
    private val activationFlow: DeviceActivationFlow,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TvSignInViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val propertyId: String = "",
        val loading: Boolean = true,
        val qrCode: Bitmap? = null,
        val userCode: String? = null,
        val bgImageUrl: FabricUrl? = null,
        val logoUrl: FabricUrl? = null,
    )

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
                        logoUrl = property.loginInfo?.logoUrl,
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
        activationDataDisposable = activationFlow.observeActivationData(propertyId)
            .doOnNext { observeActivationComplete(it) }
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
                        loading = false,
                    )
                }
            }
            .addTo(disposables)
    }

    private fun observeActivationComplete(activationData: ActivationCodeResponse) {
        activationCompleteDisposable?.dispose()
        activationCompleteDisposable =
            activationFlow.completeSignIn(activationData, propertyId, navArgs.provider)
                .subscribeBy(
                    onComplete = {
                        Log.d("Auth complete; navigating to onSignedInTarget.")
                        navArgs.onSignedInTarget?.let { navigateTo(it.asReplace()) }
                    },
                )
                .addTo(disposables)
    }
}
