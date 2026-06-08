package app.eluvio.mobile.screens.signin

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.DeviceActivationFlow
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.util.logging.Log
import com.stavfx.nav3hiltvm.annotations.HiltNavArgViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

/**
 * Mobile sign-in. Backs a translucent fullscreen-dialog entry that overlays the previous
 * screen — see [SignInScreen]. Uses the wallet web app's `response=redirect` mode: the
 * wallet hard-redirects to [AUTH_CALLBACK_URL] with `?elvToken=<token>` on success.
 *
 * This VM is flow-agnostic: [SignInScreen]/[SignInFlow] own how the browser is launched and how
 * the redirect is captured (Auth Tab result vs. `elvauth://` deep-link intent on the activity);
 * the VM just fetches the activation URL and turns a [SignInResult] into token storage + nav.
 *
 * The activation-code fetch is kicked off from [init] (not [onResume]) on [bgDisposables] so the
 * URL is ready by the time Compose composes — saves the construction → composition gap.
 */
@HiltNavArgViewModel
open class MobileSignInViewModel(
    @NavArg private val navArgs: SignInNavArgs,
    private val activationFlow: DeviceActivationFlow,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MobileSignInViewModel.State>(State(), savedStateHandle) {

    data class State(
        val signInUrl: String? = null,
        /** True once the auth token is in hand; tells the screen not to re-launch sign-in while the prefetch is in flight. */
        val loadingContent: Boolean = false,
    )

    private val propertyId = navArgs.propertyId
    private val provider = navArgs.provider

    /** Survives onResume/onPause; cleared on VM teardown. Holds the activation + completion flows. */
    private val bgDisposables = CompositeDisposable()

    init {
        observeActivationData()
    }

    /** Handles the outcome of the [SignInFlow], whichever browser mechanism produced it. */
    fun onSignInResult(result: SignInResult) {
        when (result) {
            is SignInResult.Captured -> completeSignIn(result.uri)
            SignInResult.Cancelled -> {
                Log.d("Sign-in cancelled — navigating back")
                navigateTo(NavigationEvent.GoBack)
            }
        }
    }

    /**
     * Completes sign-in from a captured redirect URI. The `elvToken` query param is a base58
     * JSON envelope built by elv-client-js; the activation flow decodes it and writes all token
     * fields (fabric, refresh, address, email, ...) to the
     * [app.eluvio.wallet.data.stores.TokenStore].
     */
    private fun completeSignIn(uri: Uri) {
        val clientAuthToken = uri.getQueryParameter("elvToken")
        if (clientAuthToken.isNullOrBlank()) {
            Log.w("Redirect URI missing elvToken: $uri — treating as cancel")
            navigateTo(NavigationEvent.GoBack)
            return
        }
        Log.d("Got clientAuthToken from redirect — completing sign-in")
        updateState { copy(loadingContent = true) }

        activationFlow.completeSignInWithAuthToken(
            clientAuthToken = clientAuthToken,
            propertyId = propertyId,
            provider = provider,
        )
            .subscribeBy(
                onError = { Log.e("completeSignInWithAuthToken errored", it) },
                onComplete = {
                    Log.d("Mobile sign-in complete — navigating")
                    val event = navArgs.onSignedInTarget?.asReplace() ?: NavigationEvent.GoBack
                    navigateTo(event)
                },
            )
            .addTo(bgDisposables)
    }

    private fun observeActivationData() {
        activationFlow.observeActivationData(propertyId, redirect = AUTH_CALLBACK_URL)
            .firstElement()
            .subscribeBy(
                onError = { Log.e("observeActivationData errored", it) },
                onSuccess = {
                    Log.d("Activation code received: ${it.code} (exp=${it.expiration}) url=${it.url}")
                    updateState { copy(signInUrl = it.url) }
                },
            )
            .addTo(bgDisposables)
    }

    override fun onCleared() {
        bgDisposables.dispose()
        super.onCleared()
    }
}

/**
 * Sent to the wallet web app as `&redirect=` so it can `window.location` to here on
 * sign-in success with `?elvToken=<token>` appended. Defined by [AuthRedirect] so the Auth
 * Tab scheme, the manifest `<intent-filter>`, and this URL can't drift apart.
 */
private const val AUTH_CALLBACK_URL = AuthRedirect.URL
