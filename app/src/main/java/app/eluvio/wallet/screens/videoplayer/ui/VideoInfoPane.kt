package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import app.eluvio.wallet.R
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import coil.load

class VideoInfoPane @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val restartButton: View
    private val title: TextView
    private val subtitle: TextView
    private val thumbnail: ImageView

    init {
        inflate(context, R.layout.view_video_info_pane, this)
        restartButton = findViewById(R.id.video_player_info_play_from_beginning)
        title = findViewById(R.id.video_player_info_title)
        subtitle = findViewById(R.id.video_player_info_subtitle)
        thumbnail = findViewById(R.id.video_player_info_thumbnail)
    }

    fun setOnRestartClickListener(listener: OnClickListener) {
        restartButton.setOnClickListener(listener)
    }

    fun animateShow() {
        isVisible = true
        restartButton.requestFocus()
        alpha = 0f
        translationY = height.toFloat()
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .start()
    }

    fun setDisplaySettings(display: DisplaySettings) {
        title.text = display.title
        subtitle.text = display.subtitle ?: display.description
        val (url, ratio) = display.thumbnailUrlAndRatio ?: (null to AspectRatio.WIDE)
        thumbnail.load(url)
        thumbnail.layoutParams.width = (thumbnail.layoutParams.height * ratio).toInt()
    }
}
