package app.eluvio.wallet.util.coil

import androidx.core.graphics.drawable.toDrawable
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.util.ThumbHash
import coil.intercept.Interceptor
import coil.request.ImageResult

/**
 * Coil interceptor that handles [FabricUrl] data with ThumbHash placeholders.
 * Automatically decodes the ThumbHash and sets it as a placeholder while the actual image loads.
 */
class FabricImageInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data

        if (data is FabricUrl) {
            val builder = chain.request.newBuilder()
                .data(data.url)

            if (data.imageHash != null) {
                val placeholder = ThumbHash.decode(data.imageHash)
                    ?.toDrawable(chain.request.context.resources)

                builder.placeholder(placeholder)
            }

            return chain.proceed(builder.build())
        }

        return chain.proceed(chain.request)
    }
}
