package app.eluvio.wallet.screens.videoplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.MediaEntity

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

    fun setStreams(streams: List<MediaEntity>, currentMediaItemId: String?) {
        adapter.setItems(streams, currentMediaItemId)
    }

    fun setOnStreamSelectedListener(listener: (MediaEntity) -> Unit) {
        adapter.onStreamSelected = listener
    }

    fun animateShow() {
        isVisible = true
        alpha = 0f
        translationY = height.toFloat()
        // Scroll to first item
        recyclerView.scrollToPosition(0)
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .withEndAction {
                // Focus first button after animation
                recyclerView.findViewHolderForAdapterPosition(0)?.itemView
                    ?.findViewById<MediaGlowTextButton>(R.id.stream_name)?.requestFocus()
            }
            .start()
    }
}
