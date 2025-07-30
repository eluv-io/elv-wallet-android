package app.eluvio.wallet.screens.home

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import app.eluvio.wallet.data.stores.DeeplinkStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asNewRoot
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.NavGraphs
import app.eluvio.wallet.screens.destinations.DashboardDestination
import app.eluvio.wallet.screens.destinations.HomeDestination
import app.eluvio.wallet.screens.destinations.NftClaimDestination
import app.eluvio.wallet.screens.destinations.VideoPlayerActivityDestination
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.spec.Direction
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val deeplinkStore: DeeplinkStore,
    private val authenticationService: AuthenticationService,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<HomeViewModel.State>(State()) {
    data class State(
        // Loading state isn't shown by default, since usually this screen is only shown for a
        // moment while figuring out where to go next.
        val showLoading: Boolean = false,
    )

    private val navArgs = HomeDestination.argsFrom(savedStateHandle)

    override fun onResume() {
        super.onResume()
        Log.v("Started Home with args: $navArgs")

        Maybe.fromCallable { navArgs.toDeeplinkRequest() }
            // There's a small risk of a race-condition here, where the install referrer is still
            // being processed by the time we get here. This isn't handled at this point.
            .switchIfEmpty(deeplinkStore.consumeDeeplinkRequest())
            .subscribeBy(
                onSuccess = {
                    // Some deeplink was found, either from NavArgs, or db.
                    handleDeeplink(it)
                },
                onComplete = {
                    // No Deeplink, proceed with normal flow
                    navigateTo(DashboardDestination.asNewRoot())
                },
                onError = { }
            )
            .addTo(disposables)
    }

    private fun handleDeeplink(deepLink: DeeplinkRequestEntity) {
        if (tokenStore.isLoggedIn && deepLink.jwt == null) {
            Log.d("Deeplink has no JWT, but user is logged in. Navigating to the deeplink without re-authenticating.")
            navigateToDeeplink(deepLink)
        } else {
            Log.d("Starting authentication using token from deeplink...")
            // We need to authenticate with the deeplink JWT, this could take a moment,
            // so show a loading state in the meanwhile.
            updateState { State(showLoading = true) }

            tokenStore.wipe()
            tokenStore.idToken.set(deepLink.jwt)

            authenticationService.getFabricTokenExternal()
                .subscribeBy(
                    onSuccess = {
                        Log.d("Successfully got fabric token from deeplink jwt: $it")
                        navigateToDeeplink(deepLink)
                    },
                    onError = {
                        Log.e("Failed to get fabric token", it)
                        // We failed to get a fabric token, so we need to re-authenticate.
                        navigateTo(NavGraphs.authFlowGraph.asNewRoot())
                    }
                )
                .addTo(disposables)
        }
    }

    private fun navigateToDeeplink(deepLink: DeeplinkRequestEntity) {
        navigateTo(DashboardDestination.asNewRoot())
        when (deepLink.action) {
            "items" -> deepLink.toNftClaimDestination()
            "play" -> deepLink.toVideoPlayerDestination()
            else -> {
                Log.e("Unknown action: ${deepLink.action}")
                null
            }
        }?.let { navigateTo(it.asPush()) }
    }

    private fun DeeplinkRequestEntity.toNftClaimDestination(): Direction? {
        return NftClaimDestination(
            marketplace = marketplace ?: return null,
            sku = sku ?: return null,
            signedEntitlementMessage = entitlement,
            backLink = backLink
        )
    }

    private fun DeeplinkRequestEntity.toVideoPlayerDestination(): Direction {
        //TODO: fix this hack once we figure out what we actually want to do with ://play actions
        return VideoPlayerActivityDestination(
            mediaItemId = "fake - won't be used",
            deeplinkhack_contract = contract
        )
    }
}
