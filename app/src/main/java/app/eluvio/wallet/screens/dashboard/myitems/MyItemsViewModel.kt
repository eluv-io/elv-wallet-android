package app.eluvio.wallet.screens.dashboard.myitems

import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.entities.NftEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.mapNotNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class MyItemsViewModel @Inject constructor(
    private val contentStore: ContentStore
) : BaseViewModel<MyItemsViewModel.State>(State()) {
    data class State(
        val loading: Boolean = true,
        val media: List<Media> = emptyList(),
    ) {
        data class Media(
            val key: String,
            val contractAddress: String,
            val imageUrl: String,
            val title: String,
            val subtitle: String? = null,
            // if there's only one token, we can show the token id
            val tokenId: String? = null,
            val tokenCount: Int = 1,
        )
    }

    override fun onResume() {
        super.onResume()
        contentStore.observeWalletData()
            .doOnNext {
                if (it.isFailure) {
                    fireEvent(Events.NetworkError)
                }
            }
            .mapNotNull { it.getOrNull() }
            .map { response -> response.map { nft -> nft.toMediaState() } }
            .subscribeBy(
                onNext = {
                    updateState { copy(media = it, loading = false) }
                },
                onError = {
                    Log.e("Error getting wallet data", it)
                }
            )
            .addTo(disposables)
    }

    private fun NftEntity.toMediaState() = State.Media(
        key = _id,
        contractAddress = contractAddress,
        imageUrl = imageUrl,
        title = displayName,
        subtitle = editionName,
        // If there's only one token, we can show the token id
        tokenId = tokenId,
        tokenCount = 1
    )

    /**
     * Groups the NFTs by contract address and returns a list of [State.Media] objects.
     * (unused currently, but will be needed for WWE or other "collectible" type drops)
     */
    private fun List<NftEntity>.toNftGroups(): List<State.Media> {
        return groupBy { it.contractAddress }
            .map { (contractAddress, nfts) ->
                val sampleNft = nfts.first()
                State.Media(
                    key = contractAddress,
                    contractAddress = contractAddress,
                    imageUrl = sampleNft.imageUrl,
                    title = sampleNft.displayName,
                    subtitle = sampleNft.editionName,
                    // If there's only one token, we can show the token id
                    tokenId = sampleNft.tokenId.takeIf { nfts.size == 1 },
                    tokenCount = nfts.size
                )
            }
    }
}
