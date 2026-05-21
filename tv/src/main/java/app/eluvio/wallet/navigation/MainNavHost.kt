package app.eluvio.wallet.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import app.eluvio.wallet.screens.dashboard.Dashboard
import app.eluvio.wallet.screens.deeplink.NftClaim
import app.eluvio.wallet.screens.deeplink.NftClaimNavArgs
import app.eluvio.wallet.screens.gallery.ImageGallery
import app.eluvio.wallet.screens.gallery.ImageGalleryNavArgs
import app.eluvio.wallet.screens.home.DeeplinkArgs
import app.eluvio.wallet.screens.home.Home
import app.eluvio.wallet.screens.nftdetail.NftDetail
import app.eluvio.wallet.screens.nftdetail.NftDetailNavArgs
import app.eluvio.wallet.screens.nftdetail.legacy.LegacyNftDetail
import app.eluvio.wallet.screens.nftdetail.legacy.LegacyNftDetailArgs
import app.eluvio.wallet.screens.nftdetail.legacy.LockedMediaDialog
import app.eluvio.wallet.screens.nftdetail.legacy.LockedMediaDialogNavArgs
import app.eluvio.wallet.screens.property.PropertyDetail
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.property.mediagrid.MediaGrid
import app.eluvio.wallet.screens.property.mediagrid.MediaGridNavArgs
import app.eluvio.wallet.screens.property.search.PropertySearch
import app.eluvio.wallet.screens.property.search.PropertySearchNavArgs
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideo
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideoNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePrompt
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import app.eluvio.wallet.screens.qrdialogs.externalmedia.ExternalMediaQrDialog
import app.eluvio.wallet.screens.qrdialogs.externalmedia.ExternalMediaQrDialogNavArgs
import app.eluvio.wallet.screens.qrdialogs.fulfillment.FulfillmentQrDialog
import app.eluvio.wallet.screens.qrdialogs.fulfillment.FulfillmentQrDialogNavArgs
import app.eluvio.wallet.screens.qrdialogs.generic.FullscreenQRDialog
import app.eluvio.wallet.screens.qrdialogs.generic.FullscreenQRDialogNavArgs
import app.eluvio.wallet.screens.redeemdialog.RedeemDialog
import app.eluvio.wallet.screens.redeemdialog.RedeemDialogNavArgs
import app.eluvio.wallet.screens.signin.SignIn
import app.eluvio.wallet.screens.signin.SignInNavArgs

private val FullscreenDialogProperties = DialogProperties(usePlatformDefaultWidth = false)

/**
 * Jetpack Navigation 2.8 host. Replaces compose-destinations' DestinationsNavHost.
 *
 * Each existing screen Composable takes no parameters — it grabs its NavArgs through
 * Hilt's SavedStateHandle. We just register the route class and call the Composable;
 * the typeMap (for routes with non-primitive @Serializable fields) is supplied so that
 * `handle.toRoute<T>()` inside the VM can decode the Bundle.
 */
@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DeeplinkArgs(),
        modifier = modifier,
        // Nav 2.8 defaults to fade transitions; compose-destinations swapped instantly.
        // Restore instant swaps so popUpTo-style replaces don't briefly recompose the
        // outgoing screen against its already-destroyed ViewModel state (which manifests
        // as e.g. PropertyDetail flashing empty lazy rows during a replace to PurchasePrompt).
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<DeeplinkArgs>(
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        "elvwallet://{action}/{marketplace}/{contract}/{sku}?jwt={jwt}&entitlement={entitlement}&back_link={backLink}"
                },
            ),
        ) { Home() }

        composable<NavTarget.Dashboard> { Dashboard() }

        composable<PropertyDetailNavArgs>(
            typeMap = PropertyDetailTypeMap,
        ) { PropertyDetail() }

        composable<MediaGridNavArgs>(
            typeMap = MediaGridTypeMap,
        ) { MediaGrid() }

        composable<PropertySearchNavArgs> { PropertySearch() }

        composable<UpcomingVideoNavArgs> { UpcomingVideo() }

        composable<SignInNavArgs>(
            typeMap = SignInTypeMap,
        ) { SignIn() }

        composable<PurchasePromptNavArgs>(
            typeMap = PurchasePromptTypeMap,
        ) { PurchasePrompt() }

        composable<NftDetailNavArgs> { NftDetail() }
        composable<LegacyNftDetailArgs> { LegacyNftDetail() }
        composable<ImageGalleryNavArgs> { ImageGallery() }
        composable<NftClaimNavArgs> { NftClaim() }

        dialog<LockedMediaDialogNavArgs>(
            dialogProperties = FullscreenDialogProperties,
        ) { entry -> LockedMediaDialog(entry.toRoute()) }

        dialog<FullscreenQRDialogNavArgs>(
            dialogProperties = FullscreenDialogProperties,
        ) { FullscreenQRDialog() }

        dialog<ExternalMediaQrDialogNavArgs>(
            dialogProperties = FullscreenDialogProperties,
        ) { ExternalMediaQrDialog() }

        dialog<FulfillmentQrDialogNavArgs> { FulfillmentQrDialog() }

        dialog<RedeemDialogNavArgs> { RedeemDialog() }

        // NavTarget.VideoPlayer launches VideoPlayerActivity directly — handled in ComposeNavigator.
    }
}
