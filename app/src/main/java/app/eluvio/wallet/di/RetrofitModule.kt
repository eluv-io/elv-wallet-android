package app.eluvio.wallet.di

import app.eluvio.wallet.network.adapters.AssetLinkAdapter
import app.eluvio.wallet.network.adapters.emptyStringAsNull
import app.eluvio.wallet.network.api.Auth0Api
import app.eluvio.wallet.network.api.FabricConfigApi
import app.eluvio.wallet.network.interceptors.AccessTokenInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.addAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Provides
    @TokenAwareHttpClient
    fun provideHttpClient(
        /** Requested explicitly instead of adding to to [interceptors] set, to guarantee it's added first. */
        accessTokenInterceptor: AccessTokenInterceptor,
        interceptors: Set<@JvmSuppressWildcards Interceptor>,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(accessTokenInterceptor)
            .apply {
                interceptors.forEach { addInterceptor(it) }
            }
            .build()
    }

    @Provides
    fun provideRetrofitBuilder(
        @TokenAwareHttpClient httpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit.Builder {
        return Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addAdapter(Rfc3339DateJsonAdapter().emptyStringAsNull())
            .add(AssetLinkAdapter())
            .build()
    }

    @Singleton
    @Provides
    @FabricConfig
    fun provideRetrofit(
        moshi: Moshi,
        interceptors: Set<@JvmSuppressWildcards Interceptor>
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(
                OkHttpClient.Builder()
                    .apply { interceptors.forEach { addInterceptor(it) } }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
    }

    @Singleton
    @Provides
    @Auth0
    fun provideAuth0Retrofit(
        moshi: Moshi,
        interceptors: Set<@JvmSuppressWildcards Interceptor>
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://prod-elv.us.auth0.com/")
            .client(
                OkHttpClient.Builder()
                    .apply { interceptors.forEach { addInterceptor(it) } }
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
    }

    @Provides
    fun provideConfigApi(@FabricConfig retrofit: Retrofit): FabricConfigApi = retrofit.create()

    @Provides
    fun provideAuth0Api(@Auth0 retrofit: Retrofit): Auth0Api = retrofit.create()
}

@Qualifier
annotation class Auth0

@Qualifier
annotation class FabricConfig

@Qualifier
annotation class TokenAwareHttpClient
