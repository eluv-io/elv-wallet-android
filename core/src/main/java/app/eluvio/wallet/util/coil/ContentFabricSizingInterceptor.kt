package app.eluvio.wallet.util.coil

import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Coil interceptor that adds width and height query parameters to contentfabric.io image urls.
 * This can save megabytes of bandwidth per image.
 * Inspiration: https://github.com/android/compose-samples/blob/main/Crane/app/src/main/java/androidx/compose/samples/crane/util/UnsplashSizingInterceptor.kt
 */
class ContentFabricSizingInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        val heightPx = chain.size.height.pxOrElse { -1 }
        if (heightPx > 0 &&
            data is String &&
            data.contains("contentfabric.io") &&
            !data.contains(".svg")
        ) {
            val url = data.toHttpUrl()
                .newBuilder()
                // ContentFabric has a bug where providing both width and height causes the image to
                // be distorted. Regardless, the intended behavior when providing both, is to ignore
                // one, and what we really want is to define maxWidth and maxHeight, and that's
                // something the CF doesn't support (yet?).
                .addQueryParameter("height", heightPx.toString())
                //.addQueryParameter("width", widthPx.toString())
                .build()
            val request = chain.request.newBuilder().data(url).build()
            return chain.proceed(request)
        }
        return chain.proceed(chain.request)
    }
}
