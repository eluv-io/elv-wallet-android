package app.eluvio.wallet.screens.nftdetail

import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.entities.ContractInfoEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class NftDetailViewModel @Inject constructor(
    private val navArgs: NftDetailNavArgs,
    private val contentStore: ContentStore,
) : BaseViewModel<NftDetailViewModel.State>(
    State(
        lookoutUrl = "https://explorer.contentfabric.io/address/${navArgs.contractAddress}/transactions"
    )
) {

    data class State(
        val loading: Boolean = true,
        val media: AllMediaProvider.Media? = null,
        val contractInfo: ContractInfoEntity? = null,
        val lookoutUrl: String? = null
    )

    override fun onResume() {
        super.onResume()

        contentStore.observeNft(navArgs.contractAddress, navArgs.tokenId)
            .mapNotNull { entity ->
                entity.nftTemplate?.let {
                    AllMediaProvider.Media.fromTemplate(
                        nftTemplateEntity = it,
                        imageOverride = entity.imageUrl,
                        tokenId = entity.tokenId,
                        versionHash = entity.versionHash
                    )
                }
            }
            .subscribeBy { updateState { copy(media = it, loading = false) } }
            .addTo(disposables)

        contentStore.getContractInfo(navArgs.contractAddress)
            .subscribeBy(
                onSuccess = { updateState { copy(contractInfo = it) } },
                onError = {
                    Log.e("Error fetching Contract Info for ${navArgs.contractAddress}", it)
                }
            )
            .addTo(disposables)
    }
}
