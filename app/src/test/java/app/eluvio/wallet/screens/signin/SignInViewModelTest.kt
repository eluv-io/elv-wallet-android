package app.eluvio.wallet.screens.signin

import android.graphics.Bitmap
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.DeviceActivationStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.network.api.authd.CsatResponse
import app.eluvio.wallet.network.api.authd.ActivationCodeResponse
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.testing.TestLogRule
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import com.ramcosta.composedestinations.generated.destinations.PropertyDetailDestination
import com.ramcosta.composedestinations.generated.navargs.toSavedStateHandle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SignInViewModelTest {

    @get:Rule
    val testLogRule = TestLogRule()

    val property = MediaPropertyEntity().apply {
        id = "prop123"
        loginInfo = mockk(relaxed = true)
    }

    private val propertyStore: MediaPropertyStore = mockk() {
        every { observeMediaProperty(property.id, true) }
            .returns(Flowable.just(property, property))
        every { observeMediaProperty(property.id, false) }
            .returns(Flowable.just(property))
    }
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val urlShortener: UrlShortener = mockk {
        every { shorten(any()) } answers { Single.just(firstArg()) }
    }

    private val deviceActivationStore = mockk<DeviceActivationStore> {
        every { observeActivationData(any(), any()) } returns Flowable.just(
            ActivationCodeResponse(
                "activation1",
                "passcode",
                "http://url",
                999
            )
        )
    }

    private val navArgs = SignInNavArgs(
        "ory",
        property.id,
        onSignedInDirection = PropertyDetailDestination(property.id)
    )
    private val vm = SignInViewModel(
        propertyStore = propertyStore,
        tokenStore = tokenStore,
        urlShortener = urlShortener,
        deviceActivationStore,
        savedStateHandle = navArgs.toSavedStateHandle(),
    )

    @Before
    fun setup() {
        // Mock top-level and extension functions
        mockkStatic(::generateQrCode)
        mockkStatic("app.eluvio.wallet.util.entity.PropertyEntityExtKt")
        every { generateQrCode(any()) } returns Single.just(mockk<Bitmap>())
    }

    @Test
    fun `onResume should fetch qr code and update state`() {
        // GIVEN
        // Unmock Property so we don't get a state update for bgImage
        every { propertyStore.observeMediaProperty(property.id, any()) }
            .returns(Flowable.empty())

        // WHEN
        vm.onResume()

        // THEN
        vm.state.test().assertValue { it.userCode == "activation1" }
        verify { urlShortener.shorten(any()) }
    }

    @Test
    fun `onResume should update background and logo from property`() {

        // WHEN
        vm.onResume()

        // THEN
        vm.state.test().assertValue {
            it.bgImageUrl == property.loginInfo?.backgroundImageUrl &&
                    it.logoUrl == property.loginInfo?.logoUrl
        }
    }

    @Test
    fun `on activation complete should prefetch properties and navigate`() {
        // GIVEN
        val testScheduler = TestScheduler()
        // So we can manually advance Flow.interval
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }

        val vm = spyk(vm)

        // Mock prefetching logic
        val page = mockk<MediaPageEntity>(relaxed = true)
        every { propertyStore.fetchMediaProperty(any()) } returns Completable.complete()
        every {
            any<MediaPropertyEntity>().getFirstAuthorizedPage(
                any(),
                any()
            )
        } returns Single.just(page)
        every { propertyStore.observeSections(any(), any(), any(), any()) } returns Flowable.just(
            listOf(MediaPageSectionEntity())
        )
        every { propertyStore.fetchMediaProperties(any()) } returns Completable.complete()
        every { vm.navigateTo(any()) } returns Unit

        // WHEN
        every { deviceActivationStore.checkToken(any()) } returns Maybe.just("success")
        vm.onResume()
        // Wait for polling to start
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        // THEN
        // Prefetching logic is called
        verify { propertyStore.fetchMediaProperties(false) }
        verify { propertyStore.fetchMediaProperty(property.id) }
        verify { propertyStore.observeSections(property, page, page.sectionIds, true) }

        // Post-auth navigation happens
        verify {
            vm.navigateTo(PropertyDetailDestination(property.id).asReplace())
        }
        RxJavaPlugins.reset()
    }
}
