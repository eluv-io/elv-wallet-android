package app.eluvio.wallet.screens.videoplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.eluvio.wallet.R
import app.eluvio.wallet.data.entities.MediaEntity
import coil.load

class StreamSelectionAdapter : RecyclerView.Adapter<StreamSelectionAdapter.ViewHolder>() {

    private var items: List<MediaEntity> = emptyList()
    private var currentMediaItemId: String? = null

    var onStreamSelected: ((MediaEntity) -> Unit)? = null

    fun setItems(items: List<MediaEntity>, currentMediaItemId: String?) {
        this.items = items
        this.currentMediaItemId = currentMediaItemId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item.id == currentMediaItemId)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.stream_thumbnail)
        private val nameButton: MediaGlowTextButton = itemView.findViewById(R.id.stream_name)

        fun bind(item: MediaEntity, isSelected: Boolean) {
            nameButton.text = item.name
            thumbnail.load(item.image) {
                crossfade(true)
            }

            nameButton.setOnClickListener {
                onStreamSelected?.invoke(item)
            }
        }
    }
}
