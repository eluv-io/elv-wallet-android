package app.eluvio.wallet.util.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.eluvio.wallet.di.TokenAwareHttpClient
import app.eluvio.wallet.util.logging.Log
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and parses thumbnail sprite sheets from WebVTT files.
 */
@Singleton
class ThumbnailLoader @Inject constructor(
    @param:TokenAwareHttpClient private val httpClient: OkHttpClient
) {
    /**
     * Loads a thumbnail sprite from a WebVTT URL.
     *
     * @param webVttUrl The full URL to the WebVTT file
     * @return Single that emits the parsed ThumbnailSprite, or an error if loading fails
     */
    fun loadThumbnails(webVttUrl: String): Single<ThumbnailSprite> {
        // Derive base URL from the video URI for resolving relative thumbnail paths
        return Single.fromCallable { fetchWebVtt(webVttUrl) }
            .subscribeOn(Schedulers.io())
            .map { vttContent ->
                val baseUrl = webVttUrl.substringBeforeLast("/")
                ThumbnailWebVttParser.parse(vttContent, baseUrl)
            }
            .flatMap { cues ->
                if (cues.isEmpty()) {
                    Single.just(ThumbnailSprite(emptyList(), null))
                } else {
                    // All cues typically reference the same sprite sheet image
                    val imageUrl = cues.first().imageUrl
                    loadSpriteImage(imageUrl).map { bitmap ->
                        ThumbnailSprite(cues, bitmap)
                    }
                }
            }
            .doOnError { error ->
                Log.e("Failed to load thumbnails from $webVttUrl", error)
            }
    }

    private fun fetchWebVtt(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("Failed to fetch WebVTT: ${response.code}")
        }

        // This looks like a warning, but if you remove the "?.", release builds will break.
        return response.body?.string()
            ?: throw RuntimeException("Empty WebVTT response")
    }

    private fun loadSpriteImage(imageUrl: String): Single<Bitmap> {
        return Single.fromCallable {
            Log.d("Loading sprite sheet from: $imageUrl")

            val request = Request.Builder()
                .url(imageUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to fetch sprite image: ${response.code}")
            }

            // This looks like a warning, but if you remove the "?.", release builds will break.
            val bytes = response.body?.bytes()
                ?: throw RuntimeException("Empty sprite image response")

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw RuntimeException("Failed to decode sprite image")
        }.subscribeOn(Schedulers.io())
    }
}
