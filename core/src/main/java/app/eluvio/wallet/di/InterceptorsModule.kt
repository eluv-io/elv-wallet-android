package app.eluvio.wallet.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

/**
 * Placeholder module to declare interceptor set.
 * Since all interceptors are currently for Debug builds only, this is required to compile Release.
 */
@Module
@InstallIn(SingletonComponent::class)
interface InterceptorsModule {
    @Multibinds
    fun bindEmptyInterceptorSet(): Set<Interceptor>
}
