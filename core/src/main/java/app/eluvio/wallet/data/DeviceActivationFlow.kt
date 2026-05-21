package app.eluvio.wallet.data

import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.unsaved
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Shared device-activation auth flow. The UI presentation differs per app (TV renders the
 * activation URL as a QR code, mobile loads it in a WebView), but the polling, token storage,
 * and post-auth property prefetch are identical — so they live here.
 *
 * Typical usage from a ViewModel:
 *
 *     flow.observeActivationData(propertyId)
 *         .doOnNext { /* surface activation.url + activation.code in UI state */ }
 *         .switchMapCompletable { flow.completeSignIn(it, propertyId, provider) }
 *         .subscribeBy { /* navigate to onSignedInTarget */ }
 */
@Singleton
class DeviceActivationFlow @Inject constructor(
    private val deviceActivationStore: DeviceActivationStore,
    private val tokenStore: TokenStore,
    private val propertyStore: MediaPropertyStore,
) {
    /**
     * Stream of activation codes for [propertyId]. Each emission means "show this code to the
     * user". If a code expires before sign-in completes, a fresh one is emitted.
     */
    fun observeActivationData(propertyId: String): Flowable<ActivationCodeResponse> =
        deviceActivationStore.observeActivationData(propertyId)

    /**
     * Polls until [activationData] is accepted by the server, then:
     * - records [provider] as the current login provider on [TokenStore]
     * - prefetches [propertyId]'s page+sections so the next screen has data ready.
     *
     * Also kicks off a background refresh of all properties (fire-and-forget, outlives the
     * caller's subscription).
     *
     * Polling errors retry automatically; the returned `Completable` only completes on success.
     */
    fun completeSignIn(
        activationData: ActivationCodeResponse,
        propertyId: String,
        provider: String,
    ): Completable {
        return Flowables.interval(POLL_INTERVAL)
            .doOnSubscribe { Log.d("starting to poll token for code=${activationData.code}") }
            .flatMapMaybe { deviceActivationStore.checkToken(activationData) }
            .firstOrError()
            .doOnError {
                Log.e("Activation polling error! This shouldn't happen, restarting polling.", it)
            }
            .retry()
            .doOnSuccess { token ->
                Log.d("Got a token $token")
                refreshAllPropertiesAsync()
            }
            .flatMapCompletable {
                prefetchPropertyAndSections(propertyId)
            }
            .doOnComplete {
                Log.d("Activation complete, recording login provider.")
                tokenStore.update(tokenStore.loginProvider to provider)
            }
    }

    private fun refreshAllPropertiesAsync() {
        // Slower op; not waited on, allowed to outlive the caller.
        propertyStore.fetchMediaProperties()
            .subscribeBy(
                onError = { Log.d("Error fetching properties post-auth. Non critical.", it) },
            )
            .unsaved()
    }

    private fun prefetchPropertyAndSections(propertyId: String): Completable {
        return propertyStore.fetchMediaProperty(propertyId)
            // Look up the Property in the cache and be sure it's not the pre-auth copy.
            .andThen(propertyStore.observeMediaProperty(propertyId, false))
            .firstElement()
            .flatMapSingle { property ->
                property
                    .getFirstAuthorizedPage(currentPage = null, propertyStore)
                    .map { page -> property to page }
            }
            .flatMapCompletable { (property, page) ->
                propertyStore.observeSections(property, page)
                    // Assume page Sections will never be empty.
                    .takeUntil { it.isNotEmpty() }
                    .ignoreElements()
            }
    }

    private companion object {
        private val POLL_INTERVAL = 5.seconds
    }
}
