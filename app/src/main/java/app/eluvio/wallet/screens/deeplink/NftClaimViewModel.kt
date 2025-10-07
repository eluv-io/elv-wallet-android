package app.eluvio.wallet.screens.deeplink

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.NftClaimStore
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider
import com.ramcosta.composedestinations.generated.destinations.LegacyNftDetailDestination
import com.ramcosta.composedestinations.generated.destinations.NftClaimDestination
import com.ramcosta.composedestinations.generated.destinations.NftDetailDestination
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.interval
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class NftClaimViewModel @Inject constructor(
    private val contentStore: ContentStore,
    private val nftClaimStore: NftClaimStore,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<NftClaimViewModel.State>(State()) {
    data class State(
        val loading: Boolean = true,
        val claimingInProgress: Boolean = false,
        val media: AllMediaProvider.Media? = null
    )

    private val navArgs = NftClaimDestination.argsFrom(savedStateHandle)

    override fun onResume() {
        super.onResume()

        // start by fetching all owned nfts, so we can check if the user already owns this SKU
        contentStore.fetchWalletData().ignoreElement()
            .andThen(
                contentStore.observeNftBySku(
                    navArgs.marketplace,
                    navArgs.sku,
                    navArgs.signedEntitlementMessage
                )
            )
            .subscribeBy(
                onNext = { (nftTemplate, ownership) ->
                    if (ownership != null) {
                        Log.w("user owns SKU/Entitlement")
                        navigateTo(
                            LegacyNftDetailDestination(
                                contractAddress = ownership.contractAddress,
                                tokenId = ownership.tokenId,
                                marketplaceId = navArgs.marketplace,
                                backLink = navArgs.backLink
                            ).asReplace()
                        )
                    } else if (nftTemplate != null) {
                        Log.w("user Doesn't own SKU/Entitlement")
                        updateState {
                            copy(
                                loading = false,
                                media = AllMediaProvider.Media.fromTemplate(nftTemplate)
                            )
                        }
                    }
                },
                onError = { Log.e("Error getting NFT data", it) }
            )
            .addTo(disposables)
    }

    fun claimNft() {
        state.firstOrError()
            .subscribeBy {
                val tenant = it.media?.tenant
                val contractAddress = it.media?.contractAddress
                Log.d("starting claim for tenant=$tenant, contractAddress=$contractAddress")
                if (tenant != null && contractAddress != null) {
                    claimNft(tenant, contractAddress)
                }
            }
            .addTo(disposables)
    }

    private fun claimNft(tenant: String, contractAddress: String) {
        nftClaimStore.initiateNftClaim(
            tenant,
            navArgs.marketplace,
            navArgs.sku,
            navArgs.signedEntitlementMessage
        )
            .flatMapPublisher { op -> pollClaimStatusUntilComplete(tenant, op) }
            .doOnSubscribe {
                updateState { copy(claimingInProgress = true) }
            }
            .flatMapSingle { result ->
                if (result is NftClaimStore.NftClaimResult.Success) {
                    // Re-fetch wallet data before navigating away.
                    // Otherwise NftDetail might think we don't actually own this nft.
                    contentStore.fetchWalletData().map { result }
                } else {
                    Single.just(result)
                }
            }
            .subscribeBy(
                onNext = { result ->
                    when (result) {
                        NftClaimStore.NftClaimResult.Pending -> {
                            Log.d("Still waiting for claim result for SKU: ${navArgs.sku}")
                        }

                        is NftClaimStore.NftClaimResult.Success -> {
                            val tokenId = result.tokenId
                            Log.d("SKU ${navArgs.sku} claimed successfully, navigating to tokenId: $tokenId")
                            navigateTo(NftDetailDestination(contractAddress, tokenId).asReplace())
                        }
                    }
                },
                onError = {
                    Log.e("Error claiming NFT", it)
                    updateState { copy(claimingInProgress = false) }
                    fireEvent(Events.NetworkError)
                }
            )
            .addTo(disposables)
    }

    private fun pollClaimStatusUntilComplete(
        tenant: String,
        op: String
    ): Flowable<NftClaimStore.NftClaimResult> {
        return Flowables.interval(2.seconds)
            .flatMapSingle { nftClaimStore.checkNftClaimStatus(tenant, op) }
            .takeUntil { it is NftClaimStore.NftClaimResult.Success }
    }
}
