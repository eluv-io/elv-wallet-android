package app.eluvio.wallet.screens.videoplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.eluvio.wallet.R
import coil.load

class StreamSelectionAdapter : RecyclerView.Adapter<StreamSelectionAdapter.ViewHolder>() {

    private var items: List<StreamItem> = emptyList()

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.stream_thumbnail)
        private val nameButton: MediaGlowTextButton = itemView.findViewById(R.id.stream_name)

        fun bind(item: StreamItem) {
            nameButton.text = item.title
            thumbnail.load(item.image) {
                crossfade(true)
            }

            nameButton.setOnClickListener {
                onStreamSelected?.invoke(item)
            }
        }
    }
}
