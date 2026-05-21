package app.eluvio.wallet.network.interceptors

import app.eluvio.wallet.data.stores.EnvironmentStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds a query parameter to requests if Staging flag is toggled in [EnvironmentStore].
 */
class StagingQueryParamInterceptor @Inject constructor(
    private val environmentStore: EnvironmentStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isStaging = environmentStore.stagingFlag.get()
        return if (isStaging == true && request.url.encodedPath.contains("/as/mw/")) {
            val url = request.url.newBuilder().addQueryParameter("env", "staging").build()
            chain.proceed(request.newBuilder().url(url).build())
        } else {
            chain.proceed(request)
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface Provider {
        @Binds
        @IntoSet
        fun binds(interceptor: StagingQueryParamInterceptor): Interceptor
    }
}
