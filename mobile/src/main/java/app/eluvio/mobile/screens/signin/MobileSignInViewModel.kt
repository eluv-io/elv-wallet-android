package app.eluvio.mobile.screens.signin

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.DeviceActivationFlow
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.util.logging.Log
import com.stavfx.nav3hiltvm.annotations.HiltNavKeyViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

/**
 * Mobile device-activation sign-in. Loads the activation URL in a WebView (provided to the
 * fragment via state), and concurrently polls for completion via [DeviceActivationFlow].
 *
 * On success, replaces this screen with `onSignedInTarget` if one was supplied (typical case
 * when the user tapped a property from Discover while logged out), otherwise pops back.
 */
@HiltNavKeyViewModel
open class MobileSignInViewModel(
    @NavArg private val navArgs: SignInNavArgs,
    private val activationFlow: DeviceActivationFlow,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MobileSignInViewModel.State>(State(), savedStateHandle) {

    data class State(
        val signInUrl: String? = null,
        val userCode: String? = null,
    )

    private val propertyId = navArgs.propertyId
    private val provider = navArgs.provider

    private var activationDataDisposable: Disposable? = null
    private var activationCompleteDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()
        observeActivationData()
    }

    private fun observeActivationData() {
        activationDataDisposable?.dispose()
        activationDataDisposable = activationFlow.observeActivationData(propertyId)
            .doOnNext { startPolling(it) }
            .subscribeBy { activationData ->
                updateState {
                    copy(
                        signInUrl = activationData.url,
                        userCode = activationData.code,
                    )
                }
            }
            .addTo(disposables)
    }

    private fun startPolling(activationData: ActivationCodeResponse) {
        activationCompleteDisposable?.dispose()
        activationCompleteDisposable =
            activationFlow.completeSignIn(activationData, propertyId, provider)
                .subscribeBy(
                    onComplete = {
                        val target = navArgs.onSignedInTarget
                        if (target != null) {
                            Log.d("Mobile sign-in complete; replacing with $target.")
                            navigateTo(target.asReplace())
                        } else {
                            Log.d("Mobile sign-in complete; popping back.")
                            navigateTo(NavigationEvent.GoBack)
                        }
                    },
                )
                .addTo(disposables)
    }
}
