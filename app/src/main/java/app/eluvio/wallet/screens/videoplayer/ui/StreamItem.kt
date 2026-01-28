package app.eluvio.wallet.screens.videoplayer.ui

import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.AdditionalViewEntity
import app.eluvio.wallet.data.entities.MediaEntity

/**
 * A unified sealed class for stream selection items, supporting both MediaEntity
 * (from stream selection API) and AdditionalViewEntity (from media item's additional_views).
 */
sealed class StreamItem {
    abstract val id: String
    abstract val title: String
    /** Image to display as FabricUrl (with optional thumbhash for placeholder). */
    abstract val image: FabricUrl?

    /** Stream from the stream selection API - load using media item ID */
    data class MediaItem(
        override val id: String,
        override val title: String,
        override val image: FabricUrl?,
    ) : StreamItem() {
        companion object {
            fun from(entity: MediaEntity): MediaItem {
                return MediaItem(
                    id = entity.id,
                    title = entity.name,
                    image = SimpleFabricUrl(entity.image, entity.imageHash),
                )
            }
        }
    }

    /** Stream from additional_views - load using playable hash */
    data class AdditionalView(
        override val id: String,
        override val title: String,
        override val image: FabricUrl?,
        val playableHash: String,
    ) : StreamItem() {
        companion object {
            fun from(entity: AdditionalViewEntity, index: Int): AdditionalView {
                return AdditionalView(
                    id = "additional_view_$index",
                    title = entity.title,
                    image = entity.imageUrl,
                    playableHash = entity.playableHash ?: "",
                )
            }
        }
    }
}

/** Simple FabricUrl wrapper for URLs that aren't stored as FabricUrlEntity */
private data class SimpleFabricUrl(
    override val url: String?,
    override val imageHash: String?,
) : FabricUrl
