package app.eluvio.wallet.testing

import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

/**
 * A JUnit rule that starts a [MockWebServer] and creates a [Retrofit] instance with the server's URL.
 * The server is automatically shut down when the test finishes.
 * Optional lambdas to configure different parts of the server and the client.
 */
class ApiTestingRule(
    private val serverBuilder: MockWebServer.() -> Unit = {},
    private val clientBuilder: OkHttpClient.Builder.() -> Unit = {},
    private val retrofitBuilder: Retrofit.Builder.() -> Unit = {}
) : TestWatcher() {
    lateinit var server: MockWebServer
        private set

    private lateinit var client: OkHttpClient

    lateinit var retrofit: Retrofit
        private set

    override fun starting(description: Description?) {
        server = MockWebServer().apply(serverBuilder)
        server.start()

        client = OkHttpClient.Builder()
            .apply(clientBuilder)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .apply(retrofitBuilder)
            .build()
    }

    override fun finished(description: Description?) {
        server.shutdown()
    }
}
