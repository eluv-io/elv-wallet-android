package app.eluvio.wallet.navigation

import android.os.Parcelable
import app.eluvio.wallet.data.GridContentOverride
import app.eluvio.wallet.data.PropertyLink
import app.eluvio.wallet.data.permissions.PermissionContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

// UI-framework-agnostic mirror of compose-destinations' generated Destination types,
// so non-UI code (e.g. ViewModels) can describe navigation without depending on Compose.
// Translated to a compose-destinations Direction at the navigator boundary via toDirection().
//
// Dual-annotated:
//   @Parcelize/Parcelable — needed by compose-destinations 2.x for complex navarg types in :tv.
//   @Serializable         — needed by Jetpack Navigation 2.8+ type-safe args in :mobile.
@Serializable
sealed interface NavTarget : Parcelable {
    @Parcelize
    @Serializable
    data object Dashboard : NavTarget

    @Parcelize
    @Serializable
    data class ExternalMediaQrDialog(val mediaItemId: String) : NavTarget

    @Parcelize
    @Serializable
    data class FulfillmentQrDialog(val transactionHash: String) : NavTarget

    @Parcelize
    @Serializable
    data class FullscreenQRDialog(
        val url: String,
        val title: String,
        val subtitleOverride: String? = null,
        val shortenUrl: Boolean = true,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class Home(
        val action: String? = null,
        val marketplace: String? = null,
        val contract: String? = null,
        val sku: String? = null,
        val jwt: String? = null,
        val entitlement: String? = null,
        val backLink: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class ImageGallery(val mediaEntityId: String) : NavTarget

    @Parcelize
    @Serializable
    data class LegacyNftDetail(
        val contractAddress: String,
        val tokenId: String,
        val marketplaceId: String? = null,
        val backLink: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class LockedMediaDialog(
        val name: String,
        val imageUrl: String,
        val subtitle: String? = null,
        val aspectRatio: Float,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class MediaGrid(
        val permissionContext: PermissionContext,
        val gridContentOverride: GridContentOverride? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class NftClaim(
        val marketplace: String,
        val sku: String,
        val signedEntitlementMessage: String? = null,
        val backLink: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class NftDetail(
        val contractAddress: String,
        val tokenId: String,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class PropertyDetail(
        val propertyId: String,
        val pageId: String? = null,
        val propertyLinks: ArrayList<PropertyLink> = arrayListOf(),
    ) : NavTarget

    @Parcelize
    @Serializable
    data class PropertySearch(
        val propertyId: String,
        val primaryFilter: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class PurchasePrompt(
        val permissionContext: PermissionContext,
        val pageOverride: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class RedeemDialog(
        val contractAddress: String,
        val tokenId: String,
        val offerId: String,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class SignIn(
        val provider: String,
        val propertyId: String,
        val onSignedInTarget: NavTarget? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class UpcomingVideo(
        val propertyId: String,
        val mediaItemId: String,
        val sourcePageId: String? = null,
    ) : NavTarget

    @Parcelize
    @Serializable
    data class VideoPlayer(
        val mediaItemId: String,
        val mediaTitle: String? = null,
        val propertyId: String? = null,
        val deeplinkhack_contract: String? = null,
    ) : NavTarget
}
