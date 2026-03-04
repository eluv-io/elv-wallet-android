package app.eluvio.wallet.screens.videoplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import app.eluvio.wallet.R
import coil.load

class StreamSelectionAdapter : RecyclerView.Adapter<StreamSelectionAdapter.ViewHolder>() {

    private var items: List<StreamItem> = emptyList()
    var currentlyPlayingStreamId: String = ""
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    var onStreamSelected: ((StreamItem) -> Unit)? = null

    fun setItems(items: List<StreamItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun indexOfCurrentlyPlaying(): Int {
        return items.indexOfFirst { it.id == currentlyPlayingStreamId }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.stream_thumbnail)
        private val nameButton: MediaGlowTextButton = itemView.findViewById(R.id.stream_name)

        fun bind(item: StreamItem) {
            val isPlaying = item.id == currentlyPlayingStreamId
            nameButton.text = item.title
            val checkDrawable = if (isPlaying) {
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_check)
            } else {
                null
            }
            nameButton.setCompoundDrawablesRelativeWithIntrinsicBounds(checkDrawable, null, null, null)
            nameButton.compoundDrawableTintList =
                AppCompatResources.getColorStateList(itemView.context, R.color.media_button_text_color)
            thumbnail.load(item.image) {
                crossfade(true)
            }

            nameButton.setOnClickListener {
                onStreamSelected?.invoke(item)
            }
        }
    }
}
