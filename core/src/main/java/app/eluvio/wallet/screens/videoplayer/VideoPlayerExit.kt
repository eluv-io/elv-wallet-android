package app.eluvio.wallet.screens.videoplayer

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideoNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import kotlinx.serialization.Serializable

/** Result extra key for the serialized [VideoPlayerExit] JSON. */
const val VIDEO_PLAYER_EXIT_EXTRA = "video_player_exit_json"

/**
 * Where the app should go once the video player closes.
 *
 * Up Next offers items the viewer isn't entitled to - the server hands over the run regardless of
 * entitlement - and live events that haven't started. Neither can be played, and the player isn't
 * part of the nav backstack, so it hands the destination back to whoever launched it instead.
 */
@Serializable
sealed interface VideoPlayerExit {
    val destination: NavKey

    @Serializable
    data class Purchase(val args: PurchasePromptNavArgs) : VideoPlayerExit {
        override val destination: NavKey get() = args
    }

    @Serializable
    data class Upcoming(val args: UpcomingVideoNavArgs) : VideoPlayerExit {
        override val destination: NavKey get() = args
    }
}
