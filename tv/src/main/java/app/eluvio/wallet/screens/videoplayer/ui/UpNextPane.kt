package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.display.thumbnailUrlAndRatio
import coil3.load

/**
 * Card shown over the player when an item finishes, counting down to the next one.
 * Play now advances immediately, Cancel declines this ending.
 */
class UpNextPane @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val countdownLabel: TextView
    private val thumbnail: ImageView
    private val title: TextView
    private val subtitle: TextView
    private val cancelButton: TextView
    private val playButton: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var remainingS = COUNTDOWN_S

    var onCancel: (() -> Unit)? = null
    var onPlay: (() -> Unit)? = null

    private val tick = object : Runnable {
        override fun run() {
            remainingS--
            if (remainingS <= 0) {
                onPlay?.invoke()
                return
            }
            countdownLabel.text = context.getString(R.string.up_next_countdown, remainingS)
            handler.postDelayed(this, 1000)
        }
    }

    init {
        inflate(context, R.layout.view_up_next_pane, this)
        countdownLabel = findViewById(R.id.up_next_countdown)
        thumbnail = findViewById(R.id.up_next_thumbnail)
        title = findViewById(R.id.up_next_title)
        subtitle = findViewById(R.id.up_next_subtitle)
        cancelButton = findViewById(R.id.up_next_cancel)
        playButton = findViewById(R.id.up_next_play)

        cancelButton.setOnClickListener { onCancel?.invoke() }
        playButton.setOnClickListener { onPlay?.invoke() }
    }

    /** Shows the card for [media] and starts the countdown. */
    fun show(media: MediaEntity) {
        val display = media.requireDisplaySettings()
        title.text = display.title
        subtitle.text = display.subtitle ?: display.headers.joinToString("   ")
        subtitle.isVisible = !subtitle.text.isNullOrEmpty()
        // The card's image slot is a fixed 16:9 box, so a landscape thumbnail fits it best.
        thumbnail.load(display.thumbnailLandscapeUrl?.url ?: display.thumbnailUrlAndRatio?.first)

        remainingS = COUNTDOWN_S
        countdownLabel.text = context.getString(R.string.up_next_countdown, remainingS)

        isVisible = true
        playButton.requestFocus()
        handler.postDelayed(tick, 1000)
    }

    fun hide() {
        handler.removeCallbacks(tick)
        isVisible = false
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    companion object {
        private const val COUNTDOWN_S = 10
    }
}
