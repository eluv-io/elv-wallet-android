package app.eluvio.wallet.screens.dashboard.myitems

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider.Media
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.replayingshare.replayingShare
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.BehaviorProcessor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MyItemsViewModel @Inject constructor(
    private val contentStore: ContentStore,
    private val propertyStore: MediaPropertyStore,
    tokenStore: TokenStore,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MyItemsViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val allMedia: AllMediaProvider.State = AllMediaProvider.State(),
        val properties: List<PropertyInfo> = emptyList(),
        val selectedProperty: PropertyInfo? = null,
    ) {
        @Immutable
        data class PropertyInfo(
            val name: String,
            val id: String,
        )
    }

    private val selectedProperty =
        BehaviorProcessor.createDefault(Optional.empty<State.PropertyInfo>())
    private val query = BehaviorProcessor.createDefault("")

    private val walletAddress = tokenStore.walletAddress.observe()
        .distinctUntilChanged()
        // Retain last value even after all subscribers have unsubscribed.
        .replayingShare()

    override fun onResume() {
        super.onResume()

        query.onNext("")
        selectedProperty.onNext(Optional.empty())

        resetOnAccountChange()

        selectedProperty.distinctUntilChanged()
            .combineLatest(query.debounce(300, TimeUnit.MILLISECONDS))
            .doOnNext { (property, _) ->
                updateState {
                    copy(
                        allMedia = AllMediaProvider.State(loading = true),
                        selectedProperty = property.orDefault(null)
                    )
                }
            }
            .switchMap { (selectedProperty, query) ->
                val propertyId = selectedProperty.orDefault(null)?.id
                contentStore.search(propertyId, displayName = query)
            }
            .map { response ->
                val allMedia = response.mapNotNull { nft ->
                    nft.nftTemplate?.let { template ->
                        Media.fromTemplate(template, nft.imageUrl, nft.tokenId)
                    }
                }
                AllMediaProvider.State(loading = false, allMedia)
            }
            .subscribeBy(
                onNext = { allMedia ->
                    updateState {
                        copy(allMedia = allMedia)
                    }
                },
                onError = {
                    fireEvent(Events.NetworkError)
                    updateState { copy(allMedia = allMedia.copy(loading = false)) }
                    Log.e("Error getting wallet data", it)
                }
            )
            .addTo(disposables)

        propertyStore.observeOwnedProperties()
            .distinctUntilChanged()
            .map {
                it.properties.map { (id, name) -> State.PropertyInfo(id = id, name = name) }
            }
            .subscribeBy(
                onNext = { properties -> updateState { copy(properties = properties) } },
                onError = { Log.e("Error observing owned properties", it) }
            )
            .addTo(disposables)
    }

    private fun resetOnAccountChange() {
        walletAddress
            .scan { oldAddress, newAddress ->
                if (oldAddress != newAddress) {
                    // Reset state to prevent data leaks between accounts.
                    Log.e("Wallet address changed. Resetting state.")
                    updateState { State() }
                    selectedProperty.onNext(Optional.empty())
                    query.onNext("")
                }
                newAddress
            }
            .subscribe()
            .addTo(disposables)
    }

    fun onPropertySelected(propertyInfo: State.PropertyInfo?) {
        val nextValue = propertyInfo?.takeIf { selectedProperty.value?.orDefault(null) != it }
        selectedProperty.onNext(Optional.of(nextValue))
    }

    fun onQueryChanged(query: String) {
        this.query.onNext(query)
    }
}
