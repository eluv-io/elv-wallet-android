package app.eluvio.wallet.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber


@Module
@InstallIn(SingletonComponent::class)
object InterceptorsModule {
    @Provides
    @IntoSet
    fun provideLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }
            .setLevel(HttpLoggingInterceptor.Level.BODY)
    }
}
