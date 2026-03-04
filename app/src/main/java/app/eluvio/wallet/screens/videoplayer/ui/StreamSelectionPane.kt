package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.eluvio.wallet.R

class StreamSelectionPane @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val recyclerView: RecyclerView
    private val adapter = StreamSelectionAdapter()

    init {
        inflate(context, R.layout.view_stream_selection_pane, this)
        recyclerView = findViewById(R.id.stream_selection_list)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    fun setStreams(streams: List<StreamItem>) {
        adapter.setItems(streams)
    }

    fun setCurrentlyPlayingStreamId(streamId: String) {
        adapter.currentlyPlayingStreamId = streamId
    }

    fun setOnStreamSelectedListener(listener: (StreamItem) -> Unit) {
        adapter.onStreamSelected = listener
    }

    fun animateShow() {
        isVisible = true
        alpha = 0f
        translationY = height.toFloat()

        val currentIndex = adapter.indexOfCurrentlyPlaying().coerceAtLeast(0)
        recyclerView.scrollToPosition(currentIndex)

        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .withEndAction {
                recyclerView.findViewHolderForAdapterPosition(currentIndex)?.itemView
                    ?.findViewById<MediaGlowTextButton>(R.id.stream_name)?.requestFocus()
            }
            .start()
    }
}
