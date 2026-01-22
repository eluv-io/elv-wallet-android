package app.eluvio.wallet.screens.videoplayer

import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.screens.videoplayer.ui.StreamItem
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.saveTo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import javax.inject.Inject

class StreamSelectionLoader @Inject constructor(
    private val apiProvider: ApiProvider,
    private val contentStore: ContentStore,
    private val realm: Realm,
) {
    fun getStreams(mediaItemId: String, propertyId: String?): Single<List<StreamItem>> {
        val additionalViewsStream = contentStore.observeMediaItem(mediaItemId)
            .firstOrError()
            .map { media ->
                media.additionalViews.mapIndexed { index, view ->
                    StreamItem.AdditionalView.from(view, index)
                }
            }
            .onErrorReturn { emptyList() }

        val apiStreams = if (propertyId != null) {
            apiProvider.getApi(MediaWalletV2Api::class)
                .flatMap { api -> api.getStreamSelections(propertyId) }
                .zipWith(apiProvider.getFabricEndpoint())
                .map { (response, baseUrl) ->
                    response.contents.orEmpty()
                        .mapNotNull { it.toEntity(baseUrl) }
                }
                .saveTo(realm)
                .map { list -> list.map { StreamItem.MediaItem.from(it) } }
                .onErrorReturn { e ->
                    Log.e("Error loading streams from API", e)
                    emptyList()
                }
        } else {
            Single.just(emptyList())
        }

        return Singles.zip(additionalViewsStream, apiStreams)
            .map { (additionalViews, api) ->
                additionalViews + api
            }
    }
}
