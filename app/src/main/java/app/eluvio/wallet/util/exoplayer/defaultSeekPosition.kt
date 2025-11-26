package app.eluvio.wallet.util.exoplayer

import androidx.media3.common.Player
import androidx.media3.common.Timeline

val Player.defaultSeekPositionMs: Long
    get() = currentTimeline.getWindow(
        currentMediaItemIndex,
        Timeline.Window()
    ).defaultPositionMs
