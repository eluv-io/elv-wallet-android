package app.eluvio.wallet.data

import app.eluvio.wallet.di.FabricConfig
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import io.reactivex.rxjava3.core.Single
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

class UrlShortener @Inject constructor(
    // Using the same Retrofit client we use for fetching Config, since the URL shortener is kind of separate from the
    // rest of the AuthD API, and doesn't require tokens.
    @FabricConfig
    private val retrofit: Retrofit
) {
    interface Api {
        @POST("https://elv.lv/tiny/create")
        fun shortenUrl(@Body url: RequestBody): Single<Map<String, Any?>>
    }

    private val api by lazy { retrofit.create<Api>() }

    fun shorten(url: String): Single<String> {
        return api.shortenUrl(url.toRequestBody()).mapNotNull {
            (it["url_mapping"] as? Map<*, *>)
                ?.get("shortened_url") as? String
        }
            .toSingle()
            .doOnSuccess { Log.d("Shortened URL: $it (original=$url)") }
            .doOnError {
                Log.e("Failed to shorten URL", it)
            }
    }
}
