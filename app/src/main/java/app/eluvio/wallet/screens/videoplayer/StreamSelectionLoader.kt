package app.eluvio.wallet.screens.videoplayer

import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.util.logging.Log
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import javax.inject.Inject

class StreamSelectionLoader @Inject constructor(
    private val apiProvider: ApiProvider,
) {
    fun getStreams(propertyId: String): Single<List<MediaEntity>> {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getStreamSelections(propertyId) }
            .zipWith(apiProvider.getFabricEndpoint())
            .map { (response, baseUrl) ->
                response.contents.orEmpty().mapNotNull { it.toEntity(baseUrl) }
            }
            // consume all errors and return empty list
            .onErrorReturn { e ->
                Log.e("Error loading streams", e)
                emptyList()
            }
    }
}
