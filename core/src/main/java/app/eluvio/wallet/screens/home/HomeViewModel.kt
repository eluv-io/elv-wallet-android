package app.eluvio.wallet.screens.home

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.core.BuildConfig
import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import app.eluvio.wallet.data.stores.DeeplinkStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asNewRoot
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.util.logging.Log
import com.stavfx.nav3hiltvm.annotations.HiltNavArgViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import app.eluvio.wallet.screens.dashboard.DashboardNavArgs
import app.eluvio.wallet.screens.deeplink.NftClaimNavArgs
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs

@HiltNavArgViewModel
open class HomeViewModel(
    @NavArg private val navArgs: DeeplinkArgs,
    private val tokenStore: TokenStore,
    private val deeplinkStore: DeeplinkStore,
    private val authenticationService: AuthenticationService,
) : BaseViewModel<HomeViewModel.State>(State()) {
    data class State(
        // Loading state isn't shown by default, since usually this screen is only shown for a
        // moment while figuring out where to go next.
        val showLoading: Boolean = false,
    )

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
                    navigateTo(startDestination().asNewRoot())
                },
                onError = { }
            )
            .addTo(disposables)
    }

    /**
     * Where to go when there's no deeplink to handle.
     *
     * For a signed-in user of a single-property build, that's the Property itself. The Dashboard
     * has nothing to offer them - Discover would only load the one Property and immediately
     * redirect here anyway - and going through it means the Dashboard chrome and a half-loaded
     * start screen flash by on every launch while that redirect resolves.
     *
     * Note this skips Discover's login-provider check, which re-authenticates when a Property
     * changes the provider out from under an existing session. That check needs the Property
     * fetched first, which is exactly the wait we're avoiding here. A stale session survives
     * until its token expires, at which point the usual 401 handling takes over.
     */
    private fun startDestination(): NavKey {
        val defaultPropertyId = BuildConfig.DEFAULT_PROPERTY_ID
        return if (defaultPropertyId != null && tokenStore.isLoggedIn) {
            PropertyDetailNavArgs(defaultPropertyId)
        } else {
            DashboardNavArgs
        }
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

            authenticationService.getFabricTokenExternal(tenantId = null)
                .subscribeBy(
                    onSuccess = {
                        Log.d("Successfully got fabric token from deeplink jwt: $it")
                        navigateToDeeplink(deepLink)
                    },
                    onError = {
                        Log.e("Failed to get fabric token", it)
                        // We failed to get a fabric token, so we just navigate to Discover.
                        navigateTo(DashboardNavArgs.asNewRoot())
                    }
                )
                .addTo(disposables)
        }
    }

    private fun navigateToDeeplink(deepLink: DeeplinkRequestEntity) {
        navigateTo(DashboardNavArgs.asNewRoot())
        when (deepLink.action) {
            "items" -> deepLink.toNftClaimTarget()
            "play" -> deepLink.toVideoPlayerTarget()
            else -> {
                Log.e("Unknown action: ${deepLink.action}")
                null
            }
        }?.let { navigateTo(it.asPush()) }
    }


    private fun DeeplinkRequestEntity.toVideoPlayerTarget(): NavKey {
        //TODO: fix this hack once we figure out what we actually want to do with ://play actions
        return VideoPlayerArgs(
            mediaItemId = "fake - won't be used",
            deeplinkhack_contract = contract
        )
    }
}

fun DeeplinkRequestEntity.toNftClaimTarget(): NavKey? {
    return NftClaimNavArgs(
        marketplace = marketplace ?: return null,
        sku = sku ?: return null,
        signedEntitlementMessage = entitlement,
        backLink = backLink
    )
}
