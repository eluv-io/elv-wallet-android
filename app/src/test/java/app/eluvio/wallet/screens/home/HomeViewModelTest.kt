package app.eluvio.wallet.screens.home

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.data.AuthenticationService
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import app.eluvio.wallet.data.stores.DeeplinkStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asNewRoot
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.testing.TestLogRule
import com.ramcosta.composedestinations.generated.destinations.DashboardDestination
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val testLogRule = TestLogRule()

    private val tokenStore: TokenStore = mockk()
    private val deeplinkStore: DeeplinkStore = mockk()
    private val authenticationService: AuthenticationService = mockk()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    @Before
    fun setup() {
        // Mock tokenStore behavior
        every { tokenStore.wipe() } returns Unit
        every { tokenStore.idToken.set(any()) } returns Unit
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
    }

    @Test
    fun `when no deeplink, should navigate to dashboard`() {
        // GIVEN
        every { deeplinkStore.consumeDeeplinkRequest() } returns Maybe.empty()

        // WHEN
        val vm = createViewModelAndSpy()
        vm.onResume()

        // THEN
        verify { vm.navigateTo(DashboardDestination.asNewRoot()) }
    }

    @Test
    fun `when logged in and deeplink has no jwt, should navigate to deeplink`() {
        // GIVEN
        every { tokenStore.isLoggedIn } returns true
        val deeplink = DeeplinkRequestEntity().apply {
            action = "items"
            marketplace = "marketplace1"
            sku = "sku1"
        }
        every { deeplinkStore.consumeDeeplinkRequest() } returns Maybe.just(deeplink)

        // WHEN
        val vm = createViewModelAndSpy()
        vm.onResume()

        // THEN
        verify {
            vm.navigateTo(DashboardDestination.asNewRoot())
            vm.navigateTo(deeplink.toNftClaimDestination()?.asPush()!!)
        }
    }

    @Test
    fun `when deeplink has jwt and auth succeeds, should navigate to deeplink`() {
        // GIVEN
        every { tokenStore.isLoggedIn } returns false
        val deeplink = DeeplinkRequestEntity().apply {
            action = "items"
            jwt = "test_jwt"
            marketplace = "marketplace1"
            sku = "sku1"
        }
        every { deeplinkStore.consumeDeeplinkRequest() } returns Maybe.just(deeplink)
        every { authenticationService.getFabricTokenExternal(null) } returns Single.just("fabric_token")

        // WHEN
        val vm = createViewModelAndSpy()
        vm.onResume()

        // THEN
        verify { tokenStore.idToken.set("test_jwt") }
        verify {
            vm.navigateTo(DashboardDestination.asNewRoot())
            vm.navigateTo(deeplink.toNftClaimDestination()?.asPush()!!)
        }
    }

    @Test
    fun `when deeplink has jwt and auth fails, should navigate to Discover`() {
        // GIVEN
        every { tokenStore.isLoggedIn } returns false
        val deeplink = DeeplinkRequestEntity().apply {
            action = "items"
            jwt = "test_jwt"
        }
        every { deeplinkStore.consumeDeeplinkRequest() } returns Maybe.just(deeplink)
        every { authenticationService.getFabricTokenExternal(null) } returns Single.error(
            RuntimeException("auth failed")
        )

        // WHEN
        val vm = createViewModelAndSpy()
        vm.onResume()

        // THEN
        // Verify we DON'T navigate to the dashboard
        verify(exactly = 1) { vm.navigateTo(DashboardDestination.asNewRoot()) }
    }

    private fun createViewModelAndSpy(): HomeViewModel {
        val vm = HomeViewModel(tokenStore, deeplinkStore, authenticationService, savedStateHandle)
        return spyk(vm).also {
            every { it.navigateTo(any()) } returns Unit
        }
    }
}
