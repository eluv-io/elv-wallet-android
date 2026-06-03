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
 * wallet hard-redirects to [AUTH_CALLBACK_URL] with `?elvToken=<token>` on success, and the
 * Auth Tab intercepts the custom scheme and delivers the URI to [onAuthCaptured].
 *
 * Because the Auth Tab is a separate activity, our activity pauses for the duration of auth
 * and resumes when the result is delivered. The activation-code fetch is kicked off from
 * [init] (not [onResume]) on [bgDisposables] so the URL is ready by the time Compose
 * composes — saves the construction → composition → onResume gap.
 */
@HiltNavArgViewModel
open class MobileSignInViewModel(
    @NavArg private val navArgs: SignInNavArgs,
    private val activationFlow: DeviceActivationFlow,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MobileSignInViewModel.State>(State(), savedStateHandle) {

    data class State(
        val signInUrl: String? = null,
        /** True once the auth token is in hand; tells the screen not to re-launch the Auth Tab while the prefetch is in flight. */
        val loadingContent: Boolean = false,
    )

    private val propertyId = navArgs.propertyId
    private val provider = navArgs.provider

    /** Survives onResume/onPause; cleared on VM teardown. Holds the activation + completion flows. */
    private val bgDisposables = CompositeDisposable()

    init {
        // run on init and not onResume so it doesn't re-trigger when the user comes back
        // from the Auth Tab.
        observeActivationData()
    }

    /**
     * Called with the Auth-Tab-captured redirect URI on successful sign-in. The `elvToken`
     * query param is a base58-encoded JSON envelope built by elv-client-js; the activation
     * flow decodes it and writes all token fields (fabric, refresh, address, email, ...) to
     * the [app.eluvio.wallet.data.stores.TokenStore].
     */
    fun onAuthCaptured(uri: Uri) {
        val clientAuthToken = uri.getQueryParameter("elvToken")
        if (clientAuthToken.isNullOrBlank()) {
            Log.w("Auth Tab callback URI missing elvToken: $uri — treating as cancel")
            navigateTo(NavigationEvent.GoBack)
            return
        }
        Log.d("Got clientAuthToken from Auth Tab callback — completing sign-in")
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

    /** Called when the Auth Tab returns without a captured redirect (user dismissed). */
    fun onAuthTabDismissed() {
        Log.d("Auth tab dismissed before auth completed — navigating back")
        navigateTo(NavigationEvent.GoBack)
    }

    private fun observeActivationData() {
        activationFlow.observeActivationData(propertyId, redirect = AUTH_CALLBACK_URL)
            .firstElement()
            .subscribeBy(
                onError = { Log.e("observeActivationData errored", it) },
                onSuccess = { activationData ->
                    Log.d("Activation code received: ${activationData.code} (exp=${activationData.expiration})")
                    updateState { copy(signInUrl = activationData.url) }
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
 * sign-in success with `?elvToken=<token>` appended. The scheme must match
 * [app.eluvio.mobile.screens.signin.AUTH_REDIRECT_SCHEME] in `SignInScreen` for the
 * Auth Tab to intercept the redirect and close.
 */
private const val AUTH_CALLBACK_URL = "elvwallet://auth-complete"
