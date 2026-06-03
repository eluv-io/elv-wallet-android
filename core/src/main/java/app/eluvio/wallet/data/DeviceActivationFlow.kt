package app.eluvio.wallet.data

import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.data.stores.login
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.network.api.authd.AuthRedirectPayload
import app.eluvio.wallet.network.api.authd.CheckTokenPayload
import app.eluvio.wallet.util.crypto.Base58
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.unsaved
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Shared wallet auth flow with two completion paths and shared post-auth prefetch:
 *  - TV: renders the activation URL as a QR code, polls the activation service for the token
 *    via [completeSignIn].
 *  - Mobile: loads the activation URL in an Auth Tab with `response=redirect`, captures the
 *    `clientAuthToken` from the redirect URI, and completes via [completeSignInWithAuthToken].
 */
@Singleton
class DeviceActivationFlow @Inject constructor(
    private val deviceActivationStore: DeviceActivationStore,
    private val tokenStore: TokenStore,
    private val propertyStore: MediaPropertyStore,
    private val moshi: Moshi,
) {
    /**
     * Stream of activation codes for [propertyId]. Each emission means "show this code to the
     * user". If a code expires before sign-in completes, a fresh one is emitted.
     *
     * @param redirect Optional post-auth redirect URL. When non-null the wallet web app is
     *   switched into `response=redirect` mode: it hard-redirects the browser to this URL with
     *   `?elvToken=<token>` appended, instead of POSTing the token to the activation service.
     *   Mobile passes a custom-scheme URL so the Auth Tab intercepts the redirect and closes
     *   instantly. TV leaves it null (QR-scanned device can't open it) and uses the polling flow.
     */
    fun observeActivationData(
        propertyId: String,
        redirect: String? = null,
    ): Flowable<ActivationCodeResponse> =
        deviceActivationStore.observeActivationData(propertyId, redirect)

    /**
     * Polls until [activationData] is accepted by the server, then prefetches [propertyId]'s
     * page+sections and records [provider] as the current login provider. Also kicks off a
     * background refresh of all properties (fire-and-forget, outlives the caller's
     * subscription). Polling errors retry automatically; the returned [Completable] only
     * completes on success.
     */
    fun completeSignIn(
        activationData: ActivationCodeResponse,
        propertyId: String,
        provider: String,
    ): Completable {
        return Flowables.interval(POLL_INTERVAL)
            .doOnSubscribe { Log.d("starting to poll token for code=${activationData.code}") }
            .flatMapMaybe { deviceActivationStore.checkToken(activationData, provider) }
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
    }

    /**
     * Redirect-flow completion: takes a `ClientAuthToken` captured from the wallet web app's
     * `&redirect=` callback URL (`?elvToken=<base58(json)>`), decodes it, stores all
     * embedded fields, then runs the same post-auth steps as [completeSignIn] —
     * refresh-all, prefetch, record provider. No polling because in `response=redirect`
     * mode the wallet doesn't POST to the activation service.
     *
     * The payload shape is produced by elv-client-js'
     * `WalletClient.ClientAuthToken() = Base58(JSON(elvToken))`.
     */
    fun completeSignInWithAuthToken(
        clientAuthToken: String,
        propertyId: String,
        provider: String,
    ): Completable {
        return Completable.fromAction {
            val payload = decodeAuthRedirectToken(clientAuthToken)
            tokenStore.login(payload, provider)
            Log.d("Stored wallet auth envelope from redirect callback (addr=${payload.address}, expiresAt=${payload.expiresAt})")
            refreshAllPropertiesAsync()
        }
            .andThen(prefetchPropertyAndSections(propertyId))
    }

    /**
     * The wallet's `ClientAuthToken` JSON uses different field names than the activation
     * service's `CheckTokenPayload` (`fabricToken` vs `token`, `address` vs `addr`), so we
     * parse to a unique type - then convert.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun decodeAuthRedirectToken(clientAuthToken: String): CheckTokenPayload {
        return moshi.adapter<AuthRedirectPayload>()
            .fromJson(String(Base58.decode(clientAuthToken)))
            ?.toCheckTokenPayload()
            ?: error("Failed to parse wallet auth redirect json payload")
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
            .doOnSubscribe { Log.d("Prefetching property+sections for $propertyId") }
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
