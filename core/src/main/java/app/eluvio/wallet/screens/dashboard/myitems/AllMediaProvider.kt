package app.eluvio.wallet.screens.dashboard.myitems

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.data.entities.NftTemplateEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.util.rx.generate
import app.eluvio.wallet.util.rx.mapNotNull
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.zipWith
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Extracted logic from MyItemsViewModel to share with other ViewModels that need the same data.
 */
class AllMediaProvider @Inject constructor(
    private val contentStore: ContentStore
) {
    @Parcelize
    data class State(
        val loading: Boolean = true,
        val media: List<Media> = emptyList(),
    ) : Parcelable

    @Parcelize
    @Immutable
    data class Media(
        val key: String,
        val contractAddress: String,
        val versionHash: String? = null,
        val imageUrl: String,
        val title: String,
        val subtitle: String? = null,
        // Doesn't show up in cards, but is used in the detail view.
        val description: String,
        // if there's only one token, we can show the token id
        val tokenId: String? = null,
        val tokenCount: Int = 1,
        val tenant: String? = null,
        val propertyId: String? = null,
    ) : Parcelable {
        companion object {
            fun fromTemplate(
                nftTemplateEntity: NftTemplateEntity,
                imageOverride: String? = null,
                tokenId: String? = null,
                versionHash: String? = null,
                tokenCount: Int = 1
            ) = Media(
                key = nftTemplateEntity.id,
                contractAddress = nftTemplateEntity.contractAddress,
                imageUrl = imageOverride ?: nftTemplateEntity.imageUrl ?: "",
                title = nftTemplateEntity.displayName,
                subtitle = nftTemplateEntity.editionName,
                description = nftTemplateEntity.description,
                // If there's only one token, we can show the token id
                tokenId = tokenId,
                versionHash = versionHash,
                // TODO: design hasn't settled on how to treat token count, this will always be 1 for now
                tokenCount = tokenCount,
                tenant = nftTemplateEntity.tenant,
                propertyId = nftTemplateEntity.propertyId,
            )
        }
    }

    fun observeAllMedia(onNetworkError: () -> Unit): Flowable<State> {
        return contentStore.observeWalletData()
            .doOnNext {
                if (it.isFailure) {
                    onNetworkError()
                }
            }
            .mapNotNull { it.getOrNull() }
            .map { response ->
                response.mapNotNull { nft ->
                    nft.nftTemplate?.let { template ->
                        Media.fromTemplate(template, nft.imageUrl, nft.tokenId)
                    }
                }
            }
            .zipWith(Flowables.generate(true) { false })
            .map { (media, firstEmit) ->
                // If the store has emitted an empty list twice, it's *actually* empty.
                val loading = firstEmit && media.isEmpty()
                State(loading, media)
            }
    }
}
