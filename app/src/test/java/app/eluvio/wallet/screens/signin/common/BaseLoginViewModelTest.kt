package app.eluvio.wallet.screens.signin.common

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.testing.RxSchedulerRule
import app.eluvio.wallet.testing.TestLogRule
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import com.ramcosta.composedestinations.generated.NavGraphs
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
import io.reactivex.rxjava3.processors.PublishProcessor
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class BaseLoginViewModelTest {
    companion object {
        @ClassRule
        @JvmField
        val rxSchedulerRule = RxSchedulerRule()
    }

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

    // Test-specific implementation of the abstract class
    class TestLoginViewModel(
        propertyStore: MediaPropertyStore,
        tokenStore: TokenStore,
        urlShortener: UrlShortener,
        savedStateHandle: SavedStateHandle,
        // Allow faking activation data and token checks
        private val activationDataFlow: Flowable<String>,
        private val checkTokenMaybe: Maybe<String>
    ) : BaseLoginViewModel<String>(
        propertyStore, tokenStore, urlShortener, LoginProviders.AUTH0, savedStateHandle
    ) {
        override fun fetchActivationData(): Flowable<String> = activationDataFlow
        override fun String.checkToken(): Maybe<*> = checkTokenMaybe
        override fun String.getPollingInterval(): Duration = 10.milliseconds
        override fun String.getQrUrl(): String = "http://qrcode.url/$this"
        override fun String.getCode(): String = "code-$this"
    }

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
        val activationDataSubject = PublishProcessor.create<String>()
        val vm = createViewModel(activationDataFlow = activationDataSubject)

        // WHEN
        vm.onResume()
        activationDataSubject.onNext("activation1")

        // THEN
        vm.state.test().assertValue { it.userCode == "code-activation1" }
        verify { urlShortener.shorten("http://qrcode.url/activation1") }
    }

    @Test
    fun `onResume should update background and logo from property`() {

        // WHEN
        val vm = createViewModel()
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
        val activationDataSubject = PublishProcessor.create<String>()
        val checkTokenSubject = PublishProcessor.create<String>()
        val vm = createViewModel(
            activationDataFlow = activationDataSubject,
            checkTokenMaybe = Maybe.just("success_token")
        ).let { spyk(it) }

        // Mock prefetching logic
        val page = mockk<MediaPageEntity>(relaxed = true)
        every { propertyStore.fetchMediaProperty(any()) } returns Completable.complete()
        every {
            any<MediaPropertyEntity>().getFirstAuthorizedPage(
                any(),
                any()
            )
        } returns Flowable.just(page)
        every { propertyStore.observeSections(any(), any(), any(), any()) } returns Flowable.just(
            listOf(MediaPageSectionEntity())
        )
        every { propertyStore.fetchMediaProperties(any()) } returns Completable.complete()
        every { vm.navigateTo(any()) } returns Unit

        // WHEN
        vm.onResume()
        // 1. Activation data is fetched
        activationDataSubject.onNext("activation1")
        Completable.timer(100, TimeUnit.MILLISECONDS).blockingAwait()
        // 2. Polling starts, and checkToken succeeds
        checkTokenSubject.onNext("success_token")

        // THEN
        // Prefetching logic is called
        verify { propertyStore.fetchMediaProperties(false) }
        // Post-auth navigation happens
        verify {
            vm.navigateTo(NavigationEvent.PopTo(NavGraphs.authFlow, true))
            vm.navigateTo(PropertyDetailDestination(property.id).asPush())
        }
    }


    private fun createViewModel(
        activationDataFlow: Flowable<String> = Flowable.never(),
        checkTokenMaybe: Maybe<String> = Maybe.empty()
    ): BaseLoginViewModel<String> {
        val savedStateHandle = SignInNavArgs(
            LoginProviders.ORY,
            property.id,
            onSignedInDirection = PropertyDetailDestination(property.id)
        ).toSavedStateHandle()
        return TestLoginViewModel(
            propertyStore = propertyStore,
            tokenStore = tokenStore,
            urlShortener = urlShortener,
            savedStateHandle = savedStateHandle,
            activationDataFlow = activationDataFlow,
            checkTokenMaybe = checkTokenMaybe
        )
    }
}
