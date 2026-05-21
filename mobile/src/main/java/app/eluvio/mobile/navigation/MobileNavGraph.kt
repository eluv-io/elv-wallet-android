package app.eluvio.mobile.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.eluvio.mobile.screens.discover.DiscoverScreen
import app.eluvio.mobile.screens.myitems.MyItemsScreen
import app.eluvio.mobile.screens.profile.ProfileScreen
import app.eluvio.mobile.screens.profile.launchEnvSwitcherIfDebug
import app.eluvio.mobile.screens.profile.rememberNetworkRowTapper
import app.eluvio.mobile.screens.property.PropertyDetailScreen
import app.eluvio.mobile.screens.signin.MobileSignInViewModel
import app.eluvio.mobile.screens.signin.SignInScreen
import app.eluvio.mobile.screens.videoplayer.MobileVideoPlayerViewModel
import app.eluvio.mobile.screens.videoplayer.VideoPlayerScreen
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavTarget
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.PropertyDetailTypeMap
import app.eluvio.wallet.navigation.SignInTypeMap
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.dashboard.discover.DiscoverViewModel
import app.eluvio.wallet.screens.dashboard.profile.ProfileViewModel
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.property.PropertyDetailViewModel
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import app.eluvio.wallet.util.subscribeToState
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.Serializable

/**
 * Top-level tab routes. The screen-specific routes (sign-in, property detail, video player)
 * reuse the shared `*NavArgs` classes from :core directly, so both apps share the same wire
 * format.
 */
@Serializable
data object DiscoverRoute

@Serializable
data object MyItemsRoute

@Serializable
data object ProfileRoute

/**
 * Wires every destination as a `composable<Route>` block. Each block delegates to a small
 * `*NavScreen` wrapper that owns the VM + state subscription. Navigation events bubble up
 * through `LocalNavigator` (provided once at the activity), so wrappers don't need a
 * NavController reference.
 */
fun NavGraphBuilder.installMobileGraph() {
    composable<DiscoverRoute> { DiscoverNavScreen() }
    composable<MyItemsRoute> { MyItemsScreen() }
    composable<ProfileRoute> { ProfileNavScreen() }
    composable<SignInNavArgs>(typeMap = SignInTypeMap) { SignInNavScreen() }
    composable<PropertyDetailNavArgs>(typeMap = PropertyDetailTypeMap) {
        PropertyDetailNavScreen()
    }
    composable<VideoPlayerArgs> { VideoPlayerNavScreen() }
}

@Composable
private fun DiscoverNavScreen() {
    val vm: DiscoverViewModel = hiltViewModel()
    vm.subscribeToState { _, state ->
        DiscoverScreen(state = state, onPropertyClick = vm::onPropertyClicked, onRetry = vm::retry)
    }
}

@Composable
private fun ProfileNavScreen() {
    val vm: ProfileViewModel = hiltViewModel()
    val tapper = rememberNetworkRowTapper()
    val context = LocalContext.current
    vm.subscribeToState { _, state ->
        ProfileScreen(
            state = state,
            onSignOut = vm::signOut,
            onNetworkRowTap = { if (tapper.tap()) context.launchEnvSwitcherIfDebug() },
        )
    }
}

@Composable
private fun SignInNavScreen() {
    val vm: MobileSignInViewModel = hiltViewModel()
    vm.subscribeToState { _, state ->
        SignInScreen(
            signInUrl = state.signInUrl,
            onNavigateUp = { vm.navigateTo(NavigationEvent.GoBack) },
        )
    }
}

@Composable
private fun PropertyDetailNavScreen() {
    val vm: PropertyDetailViewModel = hiltViewModel()
    val context = LocalContext.current
    vm.subscribeToState { _, state ->
        PropertyDetailScreen(
            state = state,
            onItemClick = { item ->
                val real = (item as? CarouselItem.BannerWrapper)?.delegate ?: item
                if (real !is CarouselItem.Media) {
                    Toast.makeText(context, "Not supported yet: $real", Toast.LENGTH_SHORT).show()
                    return@PropertyDetailScreen
                }
                when (val target = real.entity.onClickTarget(real.permissionContext)) {
                    is NavTarget.VideoPlayer -> vm.navigateTo(target.asPush())
                    null -> Toast.makeText(context, "No access to this item", Toast.LENGTH_SHORT)
                        .show()
                    else -> Toast.makeText(context, "Not supported yet: $target", Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onNavigateUp = { vm.navigateTo(NavigationEvent.GoBack) },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerNavScreen() {
    val vm: MobileVideoPlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    // MobileVideoPlayerViewModel extends ViewModel directly (not BaseViewModel) since the
    // player itself is the "state" — so no subscribeToState here. We tap LocalNavigator
    // directly for the back navigation on load errors.
    val navigator = LocalNavigator.current
    DisposableEffect(vm) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                app.eluvio.wallet.util.logging.Log.e(
                    "Error playing video ${error.errorCodeName}", error
                )
            }
        }
        vm.exoPlayer.addListener(listener)
        val disposables = CompositeDisposable()
        vm.loadErrors.subscribeBy {
            Toast.makeText(context, "Error loading video. Try again later.", Toast.LENGTH_SHORT)
                .show()
            navigator(NavigationEvent.GoBack)
        }.addTo(disposables)
        onDispose {
            vm.exoPlayer.removeListener(listener)
            disposables.clear()
        }
    }
    VideoPlayerScreen(player = vm.exoPlayer)
}

/**
 * Mobile-side resolution of the shared [NavigationEvent] vocabulary into a [NavController]
 * call. Installed once at the activity level via `LocalNavigator provides ...`. Returns
 * silently for events that have no mobile equivalent yet — those fall through to a toast
 * during dev rather than silently dropping.
 */
internal fun NavController.handleMobileNavEvent(
    event: NavigationEvent,
    onUnhandled: () -> Unit,
) {
    val route: Any = when (event) {
        NavigationEvent.GoBack -> {
            popBackStack()
            return
        }
        is NavigationEvent.Push -> event.target.toMobileRoute() ?: return onUnhandled()
        is NavigationEvent.Replace -> event.target.toMobileRoute() ?: return onUnhandled()
        is NavigationEvent.SetRoot -> event.target.toMobileRoute() ?: return onUnhandled()
    }
    when (event) {
        is NavigationEvent.Push -> navigate(route)
        is NavigationEvent.Replace -> {
            val current = currentBackStackEntry?.destination?.route
            navigate(route) {
                if (current != null) popUpTo(current) { inclusive = true }
            }
        }
        is NavigationEvent.SetRoot -> navigate(route) {
            popUpTo(graph.startDestinationId) { inclusive = true }
        }
        NavigationEvent.GoBack -> Unit
    }
}

/**
 * Maps the shared [NavTarget] vocabulary onto mobile-side @Serializable routes. Returning null
 * means "no mobile equivalent yet" — those events fall through to a toast so they're visible
 * during dev rather than silently dropped.
 */
private fun NavTarget.toMobileRoute(): Any? = when (this) {
    is NavTarget.SignIn -> SignInNavArgs(provider, propertyId, onSignedInTarget)
    is NavTarget.PropertyDetail -> PropertyDetailNavArgs(propertyId, pageId, propertyLinks)
    is NavTarget.VideoPlayer -> VideoPlayerArgs(
        mediaItemId = mediaItemId,
        mediaTitle = mediaTitle,
        propertyId = propertyId,
    )
    else -> null
}
