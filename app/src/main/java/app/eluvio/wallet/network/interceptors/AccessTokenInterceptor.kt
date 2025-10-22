package app.eluvio.wallet.network.interceptors

import android.os.Build
import android.util.Base64
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.data.stores.Installation
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.data.stores.refresh
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.AuthServicesApi
import app.eluvio.wallet.network.api.authd.RefreshCsatRequest
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.unsaved
import dagger.Lazy
import io.reactivex.rxjava3.kotlin.subscribeBy
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This interceptor is responsible for adding the appropriate token to each request.
 * As well as trying to refresh the token if it's expired, and retrying the request if the refresh is successful.
 *
 * Needs to be registered as both a (Network) [Interceptor] and an [Authenticator].
 */
@Singleton
class AccessTokenInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val signOutHandler: SignOutHandler,
    private val apiProvider: Lazy<ApiProvider>,
    private val installation: Installation,
    configStore: FabricConfigStore,
) : Interceptor, Authenticator {

    private val userAgent = if (BuildConfig.DEBUG) {
        // Don't use "androidtv" in debug build, to bypass eligible-tenants filtering by backend.
        "android-debug_v${BuildConfig.VERSION_NAME}"
    } else {
        "AndroidTV/${Build.VERSION.RELEASE} (api:${Build.VERSION.SDK_INT}; ${BuildConfig.APPLICATION_ID}:${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}; ${Build.MANUFACTURER}; ${Build.MODEL}; ${Build.DEVICE})"
    }

    /** URLs that contain these paths should not get special handling for expired tokens. */
    private val authRequestPaths = setOf("wlt/login/jwt", "wlt/sign/csat", "wlt/refresh/csat")

    private var staticToken: String? = null

    init {
        configStore.observeFabricConfiguration()
            .map { it.qspace.id }
            .distinctUntilChanged()
            .subscribeBy { qsapceId ->
                staticToken = Base64.encodeToString(
                    "{\"qspace_id\": \"${qsapceId}\"}".toByteArray(),
                    Base64.NO_WRAP
                )
                log("Updated static token: $staticToken")
            }
            .unsaved()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().withHeaders()
        val response = chain.proceed(request)
        return response
    }

    /**
     * Creates a copy of the request with all the headers we need.
     */
    private fun Request.withHeaders(): Request {
        val builder = newBuilder()
        builder.header("User-Agent", userAgent)
        builder.header("Eluvio-User-Agent", userAgent)
        builder.header("Accept", "*/*") // needed for link/file resolution from the fabric
        addAuthHeader(url.toString(), builder)
        return builder.build()
    }

    /**
     * Adds a token header appropriate for the given request URL and token availability.
     */
    private fun addAuthHeader(requestUrl: String, builder: Request.Builder) {
        if (requestUrl.contains("wlt/login/jwt")) {
            tokenStore.idToken.get()?.let { idToken ->
                log("injecting idToken into login/jwt: $idToken")
                builder.authToken(idToken)
            }
        } else if (requestUrl.contains("wlt/sign/csat")) {
            tokenStore.clusterToken.get()?.let { clusterToken ->
                log("injecting clusterToken into sign/csat: $clusterToken")
                builder.authToken(clusterToken)
            }
        } else {
            (tokenStore.fabricToken.get() ?: staticToken)?.let { fabricToken ->
                builder.authToken(fabricToken)
            }
        }
    }

    /**
     * Potentially refreshes the token and retries the request.
     */
    override fun authenticate(route: Route?, response: Response): Request? {
        // When we want to retry, we can just return the original request. No need to attach new headers.
        // Since TokenInterceptor is registered as a Network Interceptor (rather than an Application Interceptor),
        // it will be called before the new request is made, and set the right tokens again.
        val retry = response.request
        val noRetry: Request? = null

        val url = response.request.url.toString()
        val isAuthRelatedRequest = authRequestPaths.any { path -> path in url }
        if (isAuthRelatedRequest) {
            log("Auth related requested, no special handling for 401.")
            return noRetry
        }

        val refreshToken = tokenStore.refreshToken.get()
        if (refreshToken == null) {
            log("No refresh token - unable to recover from 401. Signing out.")
            return signOut()
        }

        if (response.responseCount > 1) {
            log("Tried to refresh token too many times. Giving up and signing out.")
            return signOut()
        }

        synchronized(this) {
            // use [networkResponse.request], since [request] won't have any headers set from the Network Interceptors.
            response.networkResponse?.request?.authToken
                // Check if the token has changed since the original request was made.
                ?.takeIf { originalToken -> originalToken != tokenStore.fabricToken.get() }
                ?.let {
                    log("Token changed since original request. Retrying with new token.")
                    // While we were waiting on the Synchronized block, another call must have refreshed the token.
                    return retry
                }

            try {
                val csatResponse = apiProvider.get()
                    .getApi(AuthServicesApi::class)
                    .flatMap { api ->
                        api.refreshCsat(
                            RefreshCsatRequest(
                                refreshToken,
                                installation.id,
                                tokenStore.fabricToken.get()!!,
                                // exp = 20 // Only for testing
                            )
                        )
                    }
                    .retry { count, error ->
                        count < 5 && (error is SocketTimeoutException || error is IOException)
                    }
                    .blockingGet()

                tokenStore.refresh(csatResponse)

                return retry
            } catch (e: Throwable) {
                if (e is InterruptedException || e.cause is InterruptedException) {
                    // There's no point in retrying, since this call was canceled.
                    // However, we don't really know what state we are in.
                    // Did the server get our request and burn our refresh token? Did nothing really
                    // happen? since we don't know, we're not going to sign out, and maybe the next
                    // request will succeed.
                    log("Token refresh request was interrupted. Not retrying and not signing out.")
                    return noRetry
                } else {
                    Log.e("Failed to refresh token. Signing out.", e)
                    // We failed because some other reason. Assume the refresh token is burned and
                    // there's nothing left to do but sign out.
                    return signOut()
                }
            }
        }
    }

    private fun log(message: String, throwable: Throwable? = null) {
        if (ENABLE_EXCESSIVE_LOGGING) {
            Log.d(message, throwable)
        }
    }

    /**
     * Kicks off sign out and doesn't wait for it to complete.
     * Always returns `null`. It's just a convenience so we can call `return signOut()` in a single line.
     */
    private fun signOut(): Request? {
        signOutHandler
            .signOut("Token expired. Please sign in again.")
            .doOnSubscribe { log("Sign out started") }
            .subscribeBy(
                onComplete = { log("Sign out completed.") },
                onError = { Log.e("Failed to complete sign out", it) }
            )
            .unsaved()
        return null
    }
}

// The process of refreshing a token is flaky and complicated.
// Logs are helpful for debugging, but they're too verbose to be enabled by default.
private const val ENABLE_EXCESSIVE_LOGGING = false

/**
 * Sets the authorization header, and also saves it as a Tag, so that we can check it later.
 */
private fun Request.Builder.authToken(token: String): Request.Builder {
    return this.tag(String::class.java, token)
        .header("Authorization", "Bearer $token")
}

/**
 * Retrieves the auth token from the tag if it was set.
 */
private val Request.authToken: String? get() = tag(String::class.java)

private val Response.responseCount: Int
    get() = generateSequence(this) { it.priorResponse }.count()
