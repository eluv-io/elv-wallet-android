package app.eluvio.wallet.screens.videoplayer.ui

import app.eluvio.wallet.data.entities.AdditionalViewEntity
import app.eluvio.wallet.data.entities.MediaEntity

/**
 * A unified sealed class for stream selection items, supporting both MediaEntity
 * (from stream selection API) and AdditionalViewEntity (from media item's additional_views).
 */
sealed class StreamItem {
    abstract val id: String
    abstract val label: String
    abstract val imageUrl: String

    /** Stream from the stream selection API - load using media item ID */
    data class MediaItem(
        override val id: String,
        override val label: String,
        override val imageUrl: String,
    ) : StreamItem() {
        companion object {
            fun from(entity: MediaEntity): MediaItem {
                return MediaItem(
                    id = entity.id,
                    label = entity.name,
                    imageUrl = entity.image,
                )
            }
        }
    }

    /** Stream from additional_views - load using playable hash */
    data class AdditionalView(
        override val id: String,
        override val label: String,
        override val imageUrl: String,
        val playableHash: String,
    ) : StreamItem() {
        companion object {
            fun from(entity: AdditionalViewEntity, index: Int): AdditionalView {
                return AdditionalView(
                    id = "additional_view_$index",
                    label = entity.label,
                    imageUrl = entity.imageUrl?.url ?: "",
                    playableHash = entity.playableHash ?: "",
                )
            }
        }
    }
}
