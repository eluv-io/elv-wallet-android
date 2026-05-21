package app.eluvio.wallet.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.screens.videoplayer.VIDEO_PLAYER_ARGS_EXTRA
import app.eluvio.wallet.screens.videoplayer.VideoPlayerActivity
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import kotlinx.serialization.json.Json

/**
 * Compose-side [Navigator] backed by a Nav 3 backstack list. Mutates the backstack on
 * `Push`/`Replace`/`SetRoot`/`GoBack`; the one exception is [VideoPlayerArgs], which launches
 * an Activity directly instead of being pushed as an entry.
 */
class ComposeNavigator(
    private val backStack: NavBackStack<NavKey>,
    private val context: Context,
) : Navigator {
    override fun invoke(event: NavigationEvent) {
        when (event) {
            // Pop directly rather than dispatching through OnBackPressedDispatcher. A button-click
            // "Back" should always pop one screen, not run through any registered BackHandler
            // (PropertySearch, MyItems) which is only meant to intercept hardware back.
            NavigationEvent.GoBack -> backStack.removeLastOrNull()

            is NavigationEvent.Push -> push(event.target)

            is NavigationEvent.Replace -> {
                backStack.removeLastOrNull()
                push(event.target)
            }

            is NavigationEvent.SetRoot -> {
                // Pop everything down so the new target is the only entry. Otherwise Home stays
                // in the stack underneath, and Back from the new target loops back through Home
                // (which auto-redirects to the same target — looks like a screen reload to the
                // user instead of exiting the app).
                while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                push(event.target)
            }
        }
    }

    private fun push(target: NavKey) {
        // VideoPlayer is an Activity, not a NavEntry.
        if (target is VideoPlayerArgs) {
            startVideoPlayer(target)
            return
        }
        backStack.add(target)
    }

    private fun startVideoPlayer(args: VideoPlayerArgs) {
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(VIDEO_PLAYER_ARGS_EXTRA, Json.encodeToString(VideoPlayerArgs.serializer(), args))
        }
        context.startActivity(intent)
    }
}
