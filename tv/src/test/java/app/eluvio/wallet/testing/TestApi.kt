package app.eluvio.wallet.testing

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.TestObserver
import okhttp3.ResponseBody
import retrofit2.http.GET

/**
 * A simple test retrofit endpoint. Use with a MockWebServer to mock your own responses.
 */
interface TestApi {
    @GET("test")
    fun test(): Single<ResponseBody>
}

/**
 * Calls the test endpoint, maps the response to a string, and awaits the result.
 */
fun TestApi.awaitTest(): TestObserver<String> = test().map { it.string() }.test().await()
