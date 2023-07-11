package app.eluvio.wallet.screens.nftdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.entities.MediaCollectionEntity
import app.eluvio.wallet.data.entities.MediaSectionEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.screens.destinations.NftDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class NftDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val contentStore: ContentStore
) : BaseViewModel<NftDetailViewModel.State>(State()) {
    @Immutable
    data class State(
        val title: String = "",
        val subtitle: String = "",
        val sections: List<MediaSectionEntity> = emptyList()
    )

    private val contractAddress = NftDetailDestination.argsFrom(savedStateHandle).contractAddress

    override fun onResume() {
        super.onResume()
        contentStore.observeNft(contractAddress)
            .subscribeBy(
                onNext = { nfts ->
                    val nft = nfts.first()
                    updateState {
                        State(
                            title = nft.displayName,
                            subtitle = nft.description,
                            sections = nft.mediaSections
                        )
                    }
                },
                onError = {

                })
            .addTo(disposables)
    }
}
