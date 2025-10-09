package app.eluvio.wallet.screens.signin.auth0

import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.InMemoryTokenStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.network.api.DeviceActivationData
import app.eluvio.wallet.network.api.GetTokenResponse
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.testing.RxSchedulerRule
import app.eluvio.wallet.testing.TestLogRule
import com.ramcosta.composedestinations.generated.navargs.toSavedStateHandle
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class Auth0SignInViewModelTest {
    @get:Rule
    val testLogRule = TestLogRule()

    @get:Rule
    val rxSchedulerRule = RxSchedulerRule()

    private val mediaProperty = MediaPropertyEntity().apply {
        id = "prop123"
        loginInfo = mockk(relaxed = true)
    }

    private val deviceActivationStore: DeviceActivationStore = mockk()
    private val authenticationService: AuthenticationService = mockk()
    private val propertyStore: MediaPropertyStore = mockk() {
        every { observeMediaProperty(mediaProperty.id, true) }
            .returns(Flowable.just(mediaProperty, mediaProperty))
        every { observeMediaProperty(mediaProperty.id, false) }
            .returns(Flowable.just(mediaProperty))
    }
    private val tokenStore: TokenStore = InMemoryTokenStore()

    private val urlShortener: UrlShortener = mockk {
        every { shorten(any()) } answers { Single.just(firstArg()) }
    }

    @Test
    fun `fetchActivationData should call deviceActivationStore`() {
        // GIVEN
        val activationData = createActivationData()
        every { deviceActivationStore.observeActivationData(any()) }
            .returns(Observable.just(activationData))

        // WHEN
        val vm = startViewModel()
        val testSubscriber = vm.fetchActivationData().test()

        // THEN
        testSubscriber.assertValue(activationData)
        verify { deviceActivationStore.observeActivationData(mediaProperty.loginInfo) }
        confirmVerified(deviceActivationStore)
    }

    @Test
    fun `getPollingInterval should return correct duration`() {
        // GIVEN
        val vm = startViewModel()
        val activationData = createActivationData(intervalSeconds = 10)

        // WHEN, THEN
        assertEquals(10.seconds, vm.run { activationData.getPollingInterval() })
    }

    @Test
    fun `getQrUrl should return verificationUriComplete`() {
        // GIVEN
        val vm = startViewModel()
        val activationData = createActivationData(verificationUriComplete = "http://qrcode.com")

        // WHEN, THEN
        assertEquals("http://qrcode.com", vm.run { activationData.getQrUrl() })
    }

    @Test
    fun `getCode should return userCode`() {
        // GIVEN
        val vm = startViewModel()
        val activationData = createActivationData(userCode = "ABC-123")

        // WHEN, THEN
        assertEquals("ABC-123", vm.run { activationData.getCode() })
    }

    @Test
    fun `checkToken should call through to fabric token`() {
        // GIVEN
        val activationData = createActivationData(deviceCode = "device_code_123")
        val response = Response.success(
            GetTokenResponse(
                idToken = "id_token_from_auth0",
                accessToken = "",
                refreshToken = "",
                expiresInSeconds = "3600"
            )
        )
        every { deviceActivationStore.checkToken("device_code_123", any()) }
            .returns(Single.just(response))
        every { authenticationService.getFabricToken() } returns Single.just("fabric_token")

        // WHEN
        val vm = startViewModel()
        val testSubscriber = vm.run { activationData.checkToken() }.test()

        // THEN
        testSubscriber.assertComplete()
        verify { deviceActivationStore.checkToken("device_code_123", mediaProperty.loginInfo) }
        verify { authenticationService.getFabricToken() }
        confirmVerified(authenticationService)
    }

    @Test
    fun `checkToken failure should not call getFabricToken`() {
        // GIVEN
        val activationData = createActivationData(deviceCode = "device_code_123")
        val response = Response.error<GetTokenResponse>(404, "not found".toResponseBody())
        every { deviceActivationStore.checkToken("device_code_123", any()) }
            .returns(Single.just(response))

        // WHEN
        val vm = startViewModel()
        val testSubscriber = vm.run { activationData.checkToken() }.test()

        // THEN
        testSubscriber.assertComplete() // Maybe completes without emitting a value
        verify(exactly = 0) { authenticationService.getFabricToken() }
    }

    private fun startViewModel(): Auth0SignInViewModel {
        val savedStateHandle =
            SignInNavArgs(LoginProviders.AUTH0, mediaProperty.id).toSavedStateHandle()
        return Auth0SignInViewModel(
            deviceActivationStore = deviceActivationStore,
            authenticationService = authenticationService,
            propertyStore = propertyStore,
            tokenStore = tokenStore,
            urlShortener = urlShortener,
            savedStateHandle = savedStateHandle
        ).also { it.onResume() }
    }

    private fun createActivationData(
        deviceCode: String = "",
        userCode: String = "",
        verificationUri: String = "",
        verificationUriComplete: String = "",
        intervalSeconds: Long = 5
    ) = DeviceActivationData(
        deviceCode,
        userCode,
        verificationUri,
        expiresInSeconds = 990,
        verificationUriComplete,
        intervalSeconds
    )
}
