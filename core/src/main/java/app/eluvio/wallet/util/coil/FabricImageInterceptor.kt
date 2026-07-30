package app.eluvio.wallet.util.coil

import app.eluvio.wallet.data.FabricUrl
import coil3.intercept.Interceptor
import coil3.request.ImageResult

/**
 * Coil interceptor that unwraps [FabricUrl] data into a plain url, so callers can pass the
 * typed wrapper directly to Coil.
 */
class FabricImageInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data

        if (data is FabricUrl) {
            val request = chain.request.newBuilder()
                .data(data.url)
                .build()
            return chain.withRequest(request).proceed()
        }

        return chain.proceed()
    }
}
