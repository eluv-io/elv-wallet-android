package app.eluvio.wallet.screens.nftdetail.legacy

import android.text.Html
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.MediaSectionEntity
import app.eluvio.wallet.data.entities.NftEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.FulfillmentStore
import app.eluvio.wallet.data.stores.NftNotFoundException
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.network.api.fabric.MarketplaceApi
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.toAnnotatedString
import com.google.common.base.Optional
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class LegacyNftDetailViewModel @Inject constructor(
    private val contentStore: ContentStore,
    private val fulfillmentStore: FulfillmentStore,
    private val apiProvider: ApiProvider,
    private val videoOptionsFetcher: VideoOptionsFetcher,
    private val navArgs: LegacyNftDetailArgs,
) : BaseViewModel<LegacyNftDetailViewModel.State>(State()) {
    @Immutable
    data class State(
        val title: String = "",
        val subtitle: AnnotatedString = AnnotatedString(""),
        val featuredMedia: List<MediaEntity> = emptyList(),
        val sections: List<MediaSectionEntity> = emptyList(),
        val redeemableOffers: List<Offer> = emptyList(),
        val backgroundImage: String? = null,
        val backLinkUrl: String? = null,
        val backButtonLogo: String? = null,
    ) {
        @Immutable
        data class Offer(
            val offerId: String,
            val name: String,
            val fulfillmentState: RedeemableOfferEntity.FulfillmentState,
            val contractAddress: String,
            val tokenId: String,
            val imageUrl: String?,
            val animation: MediaSource?,
        )

        fun isEmpty() =
            title.isEmpty() && featuredMedia.isEmpty() && sections.isEmpty() && redeemableOffers.isEmpty() && backgroundImage == null
    }

    private var prefetchDisposable: Disposable? = null
    private var offerLoaderDisposable: Disposable? = null

    override fun onResume() {
        super.onResume()

        apiProvider.getFabricEndpoint()
            .flatMapPublisher { endpoint ->
                contentStore.observeNft(navArgs.contractAddress, navArgs.tokenId)
                    .doOnNext { prefetchNftInfoOnce(it) }
                    .map { it to endpoint }
            }
            .doOnNext { (nft, endpoint) ->
                loadBackButtonLogo(endpoint)
                loadOffers(nft, endpoint)
            }
            .subscribeBy(
                onNext = { (nft, endpoint) ->
                    // Theoretically we could keep looking for tvBackgroundImage in [mediaSections],
                    // but we don't need to for now (I think).
                    val backgroundImage =
                        nft.featuredMedia
                            .map { it.tvBackgroundImage }
                            .firstOrNull { it.isNotEmpty() }
                            ?.let { "$endpoint$it" }
                    updateState {
                        copy(
                            title = nft.displayName,
                            subtitle = nft.descriptionRichText?.let {
                                Html.fromHtml(it).toAnnotatedString()
                            } ?: AnnotatedString(nft.description),
                            featuredMedia = nft.featuredMedia,
                            sections = nft.mediaSections,
                            backgroundImage = backgroundImage,
                            backLinkUrl = navArgs.backLink
                        )
                    }
                },
                onError = {
                    if (it is NftNotFoundException) {
                        fireEvent(Events.NftNotFound)
                        navigateTo(NavigationEvent.GoBack)
                    }
                })
            .addTo(disposables)
    }

    private fun loadBackButtonLogo(endpoint: String) {
        navArgs.marketplaceId?.let { marketplaceId ->
            apiProvider.getApi(MarketplaceApi::class)
                .flatMap { api -> api.getMarketplaceInfo(marketplaceId) }
                .mapNotNull { it.info?.branding?.tv?.logo?.path }
                .subscribeBy {
                    updateState { copy(backButtonLogo = "$endpoint$it") }
                }
                .addTo(disposables)
        }
    }

    /**
     * Loads offers with animations and images. Updates the current state once completed.
     */
    private fun loadOffers(nft: NftEntity, endpoint: String) {
        val offerStates = nft.redeemableOffers
            .filterNot { it.shouldHide }
            .mapNotNull { offer ->
                // Offers we don't have redeemState for aren't confirmed to valid to show to the user
                nft.redeemStates.firstOrNull { offer.offerId == it.offerId }
                    ?.let { redeemState -> offer to redeemState }
            }
            .map { (offer, redeemState) ->
                val fulfillmentState = offer.getFulfillmentState(redeemState)
                val animationPath = offer.animation.values.firstOrNull()
                val mediaSource: Single<Optional<MediaSource>> = if (animationPath == null) {
                    Single.just(Optional.absent())
                } else {
                    videoOptionsFetcher.fetchVideoOptionsFromPath(animationPath)
                        .map { mediaSource -> Optional.of(mediaSource) }
                        .onErrorReturnItem(Optional.absent())
                }
                mediaSource.map { optional ->
                    val imageUrl = (offer.imagePath ?: offer.posterImagePath)?.let { path ->
                        "${endpoint}${path}"
                    }
                    State.Offer(
                        offerId = offer.offerId,
                        name = offer.name,
                        fulfillmentState = fulfillmentState,
                        contractAddress = nft.contractAddress,
                        tokenId = nft.tokenId,
                        // Loaded later
                        imageUrl = imageUrl,
                        animation = optional.orNull()
                    )
                }
            }
        offerLoaderDisposable?.dispose()
        offerLoaderDisposable = if (offerStates.isEmpty()) {
            null
        } else {
            Single.zip(offerStates) { array -> array.filterIsInstance<State.Offer>() }
                .subscribeBy(
                    onSuccess = { offers ->
                        updateState { copy(redeemableOffers = offers) }
                    },
                    onError = {
                        Log.e("Error loading offerStates", it)
                    }
                )
                .addTo(disposables)
        }
    }

    /**
     * Immediately returns the nft, but won't complete until nft info is fetched from network if nft as redeemable offers.
     */
    private fun prefetchNftInfoOnce(nft: NftEntity) {
        if (prefetchDisposable == null) {
            prefetchDisposable = fulfillmentStore.refreshRedeemedOffers(nft)
                .ignoreElement()
                .doOnError { Log.w("prefetch offer info failed. This could cause problems in viewing offer $it") }
                .retry(2)
                .subscribeBy(onError = {
                    Log.e("prefetched failed and not retrying!")
                })
                .addTo(disposables)
        }
    }
}
