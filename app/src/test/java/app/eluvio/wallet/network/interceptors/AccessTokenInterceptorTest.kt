package app.eluvio.wallet.network.interceptors

import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.data.stores.InMemoryTokenStore
import app.eluvio.wallet.network.api.Auth0Api
import app.eluvio.wallet.network.api.GetTokenResponse
import app.eluvio.wallet.network.api.RefreshTokenRequest
import app.eluvio.wallet.network.dto.FabricConfiguration
import app.eluvio.wallet.network.dto.Network
import app.eluvio.wallet.network.dto.QSpace
import app.eluvio.wallet.network.dto.Services
import app.eluvio.wallet.testing.ApiTestingRule
import app.eluvio.wallet.testing.TestApi
import app.eluvio.wallet.testing.TestLogRule
import app.eluvio.wallet.testing.awaitTest
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import retrofit2.HttpException
import retrofit2.create

class AccessTokenInterceptorTest {
    @get:Rule
    val testLogRule = TestLogRule()

    @get:Rule
    val apiTestingRule = ApiTestingRule(clientBuilder = {
        authenticator(interceptor)
        addNetworkInterceptor(interceptor)
    })

    private val tokenStore = InMemoryTokenStore().apply {
        // Always start with some fabric token (logged in state)
        fabricToken.set("expired_fabric_token")
    }
    private val auth0Api = mock<Auth0Api> {
        on { refreshToken(any()) } doReturn Single.just(
            GetTokenResponse(
                "id",
                "access",
                "refresh",
                "5"
            )
        )
    }
    private val authService = mock<AuthenticationService> {
        on { getFabricToken() } doReturn Single.just("new_fabric_token")
            .doOnSuccess { tokenStore.fabricToken.set(it) }
    }
    private val signOutHandler = mock<SignOutHandler>() {
        on { signOut(any(), any()) } doReturn Completable.complete()
    }
    private val configStore = mock<FabricConfigStore> {
        on { observeFabricConfiguration() } doReturn Single.just(
            FabricConfiguration(
                "node1",
                Network(
                    Services(
                        listOf(""),
                        listOf(""),
                        listOf(""),
                    )
                ),
                QSpace("qspace_id", listOf("fakespace"))
            )
        ).toFlowable()
    }
    private val interceptor = AccessTokenInterceptor(
        tokenStore,
        auth0Api,
        { authService },
        signOutHandler,
        configStore
    )

    private val server by lazy { apiTestingRule.server }
    private val api by lazy { apiTestingRule.retrofit.create<TestApi>() }

    @Test
    fun `401 triggers token refresh, and request retried`() {
        // Start with some refresh token
        tokenStore.refreshToken.set("old_refresh_token")

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("success"))

        api.awaitTest()
            .assertValue {
                it == "success"
            }

        // New access/refresh token requested using old token
        verify(auth0Api).refreshToken(RefreshTokenRequest(refreshToken = "old_refresh_token"))
        // New fabric token generated
        verify(authService).getFabricToken()
        // New refresh token stored
        assert(tokenStore.refreshToken.get() == "refresh")
    }

    @Test
    fun `2 simultaneous calls result in a single call to refresh token`() {
        // Start with some refresh token
        tokenStore.refreshToken.set("old_refresh_token")

        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("success1"))
        server.enqueue(MockResponse().setBody("success2"))

        val results = api.test().mergeWith(api.test())
            .map { it.string() }
            .test().await()
            .values()
        assert(results.containsAll(listOf("success1", "success2")))

        verify(auth0Api, times(1)).refreshToken(any())
    }

    // Refresh token is used up, can't refresh anymore
    @Test
    fun `auth0 refresh call completes but fails - signout called`() {
        // Start with some refresh token
        tokenStore.refreshToken.set("old_refresh_token")
        server.enqueue(MockResponse().setResponseCode(401))

        // Fail the refresh token call
        auth0Api.stub {
            on { refreshToken(any()) } doReturn Single.error(RuntimeException("error"))
        }

        api.awaitTest().assertError {
            it is HttpException && it.code() == 401
        }
        // Signout called
        verify(signOutHandler).signOut(any(), any())
    }

    // Refresh in an unknown state. Give up without signing out. Maybe we'll have better luck next time.
    @Test
    fun `auth0 interrupted - no sign out`() {
        // Start with some refresh token
        tokenStore.refreshToken.set("old_refresh_token")
        server.enqueue(MockResponse().setResponseCode(401))

        // Fail the refresh token call
        auth0Api.stub {
            on { refreshToken(any()) } doReturn Single.error(InterruptedException("interrupted"))
        }

        api.awaitTest().assertError {
            it is HttpException && it.code() == 401
        }

        // Signout NOT called
        verify(signOutHandler, never()).signOut(any(), any())
    }

    // retrying too many times calls sign out
    @Test
    fun `too many retries - sign out called`() {
        // Start with some refresh token
        tokenStore.refreshToken.set("old_refresh_token")
        // Server returns 401 even after successful refresh
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        api.awaitTest().assertError {
            it is HttpException && it.code() == 401
        }

        // Signout called
        verify(signOutHandler).signOut(any(), any())
    }

    @Test
    fun `No refresh token - signout called`() {
        // Start with no refresh token
        tokenStore.refreshToken.set(null)
        server.enqueue(MockResponse().setResponseCode(401))

        api.awaitTest().assertError {
            it is HttpException && it.code() == 401
        }

        // Signout called
        verify(signOutHandler).signOut(any(), any())
    }
}
