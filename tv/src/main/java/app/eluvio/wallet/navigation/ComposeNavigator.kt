package app.eluvio.wallet.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import app.eluvio.wallet.screens.deeplink.NftClaimNavArgs
import app.eluvio.wallet.screens.gallery.ImageGalleryNavArgs
import app.eluvio.wallet.screens.home.DeeplinkArgs
import app.eluvio.wallet.screens.nftdetail.NftDetailNavArgs
import app.eluvio.wallet.screens.nftdetail.legacy.LegacyNftDetailArgs
import app.eluvio.wallet.screens.nftdetail.legacy.LockedMediaDialogNavArgs
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.property.mediagrid.MediaGridNavArgs
import app.eluvio.wallet.screens.property.search.PropertySearchNavArgs
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideoNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import app.eluvio.wallet.screens.qrdialogs.externalmedia.ExternalMediaQrDialogNavArgs
import app.eluvio.wallet.screens.qrdialogs.fulfillment.FulfillmentQrDialogNavArgs
import app.eluvio.wallet.screens.qrdialogs.generic.FullscreenQRDialogNavArgs
import app.eluvio.wallet.screens.redeemdialog.RedeemDialogNavArgs
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.screens.videoplayer.VIDEO_PLAYER_ARGS_EXTRA
import app.eluvio.wallet.screens.videoplayer.VideoPlayerActivity
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import kotlinx.serialization.json.Json

/**
 * Compose-side [Navigator] backed by Jetpack Navigation's [NavController].
 *
 * Translates [NavTarget] into the matching `*NavArgs` route class registered in [MainNavHost],
 * with the exception of [NavTarget.VideoPlayer] which launches an Activity directly.
 */
class ComposeNavigator(
    private val navController: NavController,
    private val context: Context,
) : Navigator {
    override fun invoke(event: NavigationEvent) {
        when (event) {
            // Pop directly rather than dispatching through OnBackPressedDispatcher. A button-click
            // "Back" should always pop one screen, not run through any registered BackHandler
            // (PropertySearch, MyItems) which is only meant to intercept hardware back. Routing
            // through the dispatcher also caused dialogs (ExternalMediaQrDialog) to over-pop the
            // parent screen in addition to themselves.
            NavigationEvent.GoBack -> navController.popBackStack()

            is NavigationEvent.Push -> navigate(event.target)

            is NavigationEvent.Replace -> {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                navigate(event.target) {
                    if (currentRoute != null) {
                        popUpTo(currentRoute) { inclusive = true }
                    }
                }
            }

            is NavigationEvent.SetRoot -> navigate(event.target) {
                // Pop Home inclusively so the new target is the only entry. Otherwise Home stays
                // in the stack underneath, and Back from the new target loops back through Home
                // (which auto-redirects to the same target — looks like a screen reload to the
                // user instead of exiting the app).
                popUpTo<DeeplinkArgs> { inclusive = true }
            }
        }
    }

    private fun navigate(
        target: NavTarget,
        builder: androidx.navigation.NavOptionsBuilder.() -> Unit = {},
    ) {
        // VideoPlayer is an Activity, not a Composable in the NavHost.
        if (target is NavTarget.VideoPlayer) {
            startVideoPlayer(target)
            return
        }
        val route = target.toRoute()
        navController.navigate(route, builder)
    }

    private fun startVideoPlayer(target: NavTarget.VideoPlayer) {
        val args = VideoPlayerArgs(
            mediaItemId = target.mediaItemId,
            mediaTitle = target.mediaTitle,
            propertyId = target.propertyId,
            deeplinkhack_contract = target.deeplinkhack_contract,
        )
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(VIDEO_PLAYER_ARGS_EXTRA, Json.encodeToString(VideoPlayerArgs.serializer(), args))
        }
        context.startActivity(intent)
    }
}

/**
 * Maps a [NavTarget] to its matching `*NavArgs` route class (or `NavTarget.Dashboard` itself for
 * the only no-args screen). Mirrors the old `toDirection()` mapping that returned compose-
 * destinations Directions.
 *
 * [NavTarget.VideoPlayer] has no route — it's an Activity, handled by [ComposeNavigator] directly.
 */
private fun NavTarget.toRoute(): Any = when (this) {
    NavTarget.Dashboard -> this
    is NavTarget.ExternalMediaQrDialog -> ExternalMediaQrDialogNavArgs(mediaItemId)
    is NavTarget.FulfillmentQrDialog -> FulfillmentQrDialogNavArgs(transactionHash)
    is NavTarget.FullscreenQRDialog ->
        FullscreenQRDialogNavArgs(url, title, subtitleOverride, shortenUrl)

    is NavTarget.Home ->
        DeeplinkArgs(action, marketplace, contract, sku, jwt, entitlement, backLink)

    is NavTarget.ImageGallery -> ImageGalleryNavArgs(mediaEntityId)
    is NavTarget.LegacyNftDetail ->
        LegacyNftDetailArgs(contractAddress, tokenId, marketplaceId, backLink)

    is NavTarget.LockedMediaDialog ->
        LockedMediaDialogNavArgs(name, imageUrl, subtitle, aspectRatio)

    is NavTarget.MediaGrid -> MediaGridNavArgs(permissionContext, gridContentOverride)
    is NavTarget.NftClaim ->
        NftClaimNavArgs(marketplace, sku, signedEntitlementMessage, backLink)

    is NavTarget.NftDetail -> NftDetailNavArgs(contractAddress, tokenId)
    is NavTarget.PropertyDetail -> PropertyDetailNavArgs(propertyId, pageId, propertyLinks)
    is NavTarget.PropertySearch -> PropertySearchNavArgs(propertyId, primaryFilter)
    is NavTarget.PurchasePrompt -> PurchasePromptNavArgs(permissionContext, pageOverride)
    is NavTarget.RedeemDialog -> RedeemDialogNavArgs(contractAddress, tokenId, offerId)
    is NavTarget.SignIn -> SignInNavArgs(provider, propertyId, onSignedInTarget)
    is NavTarget.UpcomingVideo ->
        UpcomingVideoNavArgs(propertyId, mediaItemId, sourcePageId)

    is NavTarget.VideoPlayer ->
        error("VideoPlayer is an Activity — should be handled by startVideoPlayer().")
}
