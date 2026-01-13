package app.eluvio.wallet.data

import android.content.Context
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.di.TokenAwareHttpClient
import app.eluvio.wallet.network.api.authd.VideoPlayoutApi
import app.eluvio.wallet.network.api.fabric.AssetFetcherApi
import app.eluvio.wallet.screens.videoplayer.VideoPlayoutInfo
import app.eluvio.wallet.screens.videoplayer.toVideoPlayoutInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import okhttp3.OkHttpClient
import javax.inject.Inject

class VideoOptionsFetcher @Inject constructor(
    private val apiProvider: ApiProvider,
    private val contentStore: ContentStore,
    @param:ApplicationContext private val context: Context,
    @param:TokenAwareHttpClient private val httpClient: OkHttpClient,
) {
    fun fetchVideoOptions(mediaItemId: String, propertyId: String?): Single<VideoPlayoutInfo> {
        return if (propertyId != null) {
            fetchVideoOptionsFromProperty(propertyId, mediaItemId)
        } else {
            contentStore.observeMediaItem(mediaItemId)
                .firstOrError()
                .flatMap { mediaItem ->
                    val hash = mediaItem.playableHash
                    if (hash != null) {
                        fetchVideoOptionsFromHash(hash)
                    } else {
                        val path = mediaItem.mediaLinks.values.firstOrNull()
                            ?: throw RuntimeException("No media link found for $mediaItemId")
                        fetchVideoOptionsFromPath(path)
                    }
                }
        }
    }

    /**
     * This is the preferred way to fetch video options. It's the only way the backend sets
     * start/end fields correctly for clip playback
     */
    private fun fetchVideoOptionsFromProperty(
        propertyId: String,
        mediaItemId: String
    ): Single<VideoPlayoutInfo> {
        return apiProvider.getApi(VideoPlayoutApi::class)
            .zipWith(apiProvider.getFabricEndpoint())
            .flatMap { (api, baseUrl) ->
                api.getVideoOptions(
                    propertyId = propertyId,
                    mediaItemId = mediaItemId
                ).map { response ->
                    response.toVideoPlayoutInfo(baseUrl, context, httpClient)
                        ?: throw RuntimeException("No supported video formats found in $response")
                }
            }
    }

    // Calls an API that fetches video options for the latest available hash, instead of directly
    // going to the fabric with the hash we have.
    fun fetchVideoOptionsFromHash(hash: String): Single<VideoPlayoutInfo> {
        return apiProvider.getApi(VideoPlayoutApi::class)
            .zipWith(apiProvider.getFabricEndpoint())
            .flatMap { (api, baseUrl) ->
                api.getVideoOptions(hash).map { response ->
                    response.toVideoPlayoutInfo(baseUrl, context, httpClient)
                        ?: throw RuntimeException("No supported video formats found in $response")
                }
            }
    }

    fun fetchVideoOptionsFromPath(fabricPath: String): Single<VideoPlayoutInfo> {
        return apiProvider.getApi(AssetFetcherApi::class)
            .flatMap {
                it.getVideoOptions(fabricPath).map { response ->
                    val url = response.raw().request.url.toString()
                    val pathDelimiter = if (url.contains("%2F")) "%2F" else "/"
                    val baseUrl = url.substringBeforeLast(pathDelimiter)
                    response.body()
                        ?.toVideoPlayoutInfo(baseUrl, context, httpClient)
                        ?: throw RuntimeException("No supported video formats found from $url")
                }
            }
    }
}
