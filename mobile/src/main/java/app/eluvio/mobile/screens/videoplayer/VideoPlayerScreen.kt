package app.eluvio.mobile.screens.videoplayer

import android.app.Activity
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

/**
 * Entry body for the video-player route. [MobileVideoPlayerViewModel] extends `ViewModel`
 * directly (not `BaseViewModel`) since the player itself is the "state" — so there's no
 * `subscribeToState` here. We tap [LocalNavigator] directly to navigate back on load errors.
 */
@Composable
internal fun VideoPlayerScreen(vm: MobileVideoPlayerViewModel) {
    val context = LocalContext.current
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    SystemBarsForOrientation(landscape)
    KeepScreenOn()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black) },
    ) {
        // Pad around system bars so controls (and back-pressed scrub bar) clear status /
        // nav / gesture bars on phones like the Galaxy. In landscape we've already hidden
        // the bars, so the inset padding is zero.
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues()),
            factory = { context ->
                PlayerView(context).apply {
                    setShowSubtitleButton(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    useController = true
                }
            },
            update = { view -> view.player = player },
            onRelease = { view -> view.player = null },
        )
    }
}

/**
 * Hides the system bars in landscape so video plays edge-to-edge; restores them in portrait.
 * Restores bars on dispose so other screens aren't left in fullscreen mode.
 */
@Composable
private fun SystemBarsForOrientation(landscape: Boolean) {
    val context = LocalContext.current
    DisposableEffect(landscape) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        if (landscape) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
