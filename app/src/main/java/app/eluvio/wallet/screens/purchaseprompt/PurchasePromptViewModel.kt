package app.eluvio.wallet.screens.purchaseprompt

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.permissions.PermissionContextResolver
import app.eluvio.wallet.data.stores.Environment
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.navigation.onClickDirection
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.navArgs
import app.eluvio.wallet.util.crypto.Base58
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.mapNotNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PurchasePromptViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    permissionContextResolver: PermissionContextResolver,
    private val environmentStore: EnvironmentStore,
    private val urlShortener: UrlShortener,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PurchasePromptViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val media: MediaEntity? = null,
        val itemPurchase: ItemPurchase? = null,
        val purchaseUrl: String? = null,
        val qrImage: Bitmap? = null,
        val bgImageUrl: FabricUrl? = null,
        val complete: Boolean = false,
    ) {
        @Immutable
        data class ItemPurchase(val displaySettings: DisplaySettings?)
    }

    private val permissionContext = savedStateHandle.navArgs<PermissionContext>()

    private val resolvedContext = permissionContextResolver.resolve(permissionContext)
        .asSharedState()

    override fun onResume() {
        super.onResume()

        updateQrCode()

        updateBackgroundImage()

        updateCardItem()

        pollForPermissionGranted()
    }

    private fun updateQrCode() {
        val permissionSettings = resolvedContext
            .map {
                Optional.of(
                    it.mediaItem?.resolvedPermissions
                        ?: it.sectionItem?.resolvedPermissions
                        ?: it.section?.resolvedPermissions
                        ?: it.page?.pagePermissions
                        ?: it.property.propertyPermissions
                )
            }
            .distinctUntilChanged()

        environmentStore.observeSelectedEnvironment()
            .firstOrError(/*Assume env won't change while in this screen*/)
            .flatMapPublisher { env ->
                permissionSettings
                    .map { permissionSettings ->
                        buildPurchaseUrl(permissionContext, env, permissionSettings.orDefault(null))
                    }
            }
            // Make sure to not re-generate the QR code if the URL hasn't changed.
            .distinctUntilChanged()
            .switchMapSingle { fullUrl ->
                // Try to shorten
                urlShortener.shorten(fullUrl)
                    // Fall back to full URL if shortening fails
                    .onErrorReturnItem(fullUrl)
            }
            .switchMapSingle { url ->
                // Generate QR for whatever URL we have
                generateQrCode(url)
                    .map { it to url }
            }
            .subscribeBy(
                onNext = { (qr, url) -> updateState { copy(purchaseUrl = url, qrImage = qr) } },
                onError = { Log.e("Error generating QR code", it) }
            )
            .addTo(disposables)
    }

    private fun updateBackgroundImage() {
        resolvedContext.mapNotNull { context ->
            // If no "page" is defined, default to the property's main page.
            val page = context.page ?: context.property.mainPage
            context.property.loginInfo?.backgroundImageUrl
                ?: page?.backgroundImageUrl
        }
            .subscribeBy(
                onNext = { updateState { copy(bgImageUrl = it) } },
                onError = { Log.e("Error loading background image", it) },
            )
            .addTo(disposables)
    }

    private fun updateCardItem() {
        resolvedContext
            .subscribeBy(
                onNext = { resolved ->
                    val itemPurchase = resolved.sectionItem?.let {
                        State.ItemPurchase(it.displaySettings)
                    }
                    updateState { copy(media = resolved.mediaItem, itemPurchase = itemPurchase) }
                },
                onError = { Log.e("Error loading purchase item", it) }
            )
            .addTo(disposables)
    }

    /**
     * Keep checking permissions to see if the user is now allowed to view the content,
     * then navigate to it.
     */
    private fun pollForPermissionGranted() {
        resolvedContext
            .mapNotNull { resolved ->
                // Depending on the context, emit a nav event for the next screen when the user gains
                // access to the relevant resource.
                when {
                    // Note: This is really the only case we are officially supporting for now.
                    // Non-media sources are technically possible, but not expected in practice.
                    resolved.mediaItem != null -> {
                        resolved.mediaItem.resolvedPermissions
                            ?.takeIf { it.authorized == true }
                            ?.let {
                                // Side effect to display "Success" state.
                                updateState { copy(complete = true) }
                                resolved.mediaItem.onClickDirection(permissionContext)
                            }
                    }

                    resolved.section != null || resolved.sectionItem != null -> {
                        // The only SectionItem type we handle auto-forward for is Media.
                        // Known unhandled types: ItemPurchase.
                        null
                    }

                    resolved.page != null -> {
                        resolved.page.pagePermissions
                            ?.takeIf { it.authorized == true }
                            ?.let {
                                PropertyDetailDestination(resolved.property.id, resolved.page.id)
                            }
                    }

                    else -> {
                        resolved.property.propertyPermissions
                            ?.takeIf { it.authorized == true }
                            ?.let { PropertyDetailDestination(resolved.property.id) }
                    }
                }
            }
            .firstOrError(
                // Once permission is granted, stop observing to prevent double-navigation.
            )
            .subscribeBy { destination ->
                // No auto-navigate for now.
                // navigateTo(destination.asReplace())
            }
            .addTo(disposables)

        propertyStore.fetchPermissionStates(permissionContext.propertyId)
            .doOnSubscribe { Log.i("Starting to fetch permission states.") }
            .doOnComplete { Log.i("Permission states fetched.") }
            .delay(5, TimeUnit.SECONDS)
            .repeat()
            .subscribe()
            .addTo(disposables)
    }
}

/**
 * Creates a Wallet URL for either purchasing a specific item, or being offered multiple items that
 * will unlock access for this [PermissionContext].
 */
private fun buildPurchaseUrl(
    permissionContext: PermissionContext,
    environment: Environment,
    permissionSettings: PermissionSettings?,
): String {
    val context = JSONObject()
        // Only supported type is "purchase" for now.
        .put("type", "purchase")
        // Start from the most broad id and keep overriding with more specific ids.
        // Any field that is [null] will not override the previous value.
        .putOpt("id", permissionContext.propertyId)
        .putOpt("id", permissionContext.pageId)
        .putOpt("id", permissionContext.sectionId)
        .putOpt("id", permissionContext.sectionItemId)
        .putOpt("id", permissionContext.mediaItemId)
        // Everything else we have explicitly specified.
        .putOpt("sectionSlugOrId", permissionContext.sectionId)
        .putOpt("sectionItemId", permissionContext.sectionItemId)
        .putOpt(
            "permissionItemIds",
            permissionSettings?.permissionItemIds
                ?.takeIf { it.isNotEmpty() }
                ?.let { JSONArray(it) }
        )
        .putOpt("secondaryPurchaseOption", permissionSettings?.secondaryMarketPurchaseOption)

    val encodedContext = Base58.encode(context.toString().toByteArray())
    return "${environment.walletUrl}/${permissionContext.propertyId}/${permissionContext.pageId.orEmpty()}?p=$encodedContext"
}
