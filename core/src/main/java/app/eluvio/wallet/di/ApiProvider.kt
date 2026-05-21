package app.eluvio.wallet.di

import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.network.api.authd.AuthdApi
import app.eluvio.wallet.network.api.fabric.FabricApi
import app.eluvio.wallet.network.dto.FabricConfiguration
import io.reactivex.rxjava3.core.Single
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * The base url we are hitting is dynamic and can change at any time.
 * This means we can't just construct a Retrofit instance at app startup and provide it with Hilt.
 */
@Singleton
class ApiProvider @Inject constructor(
    private val builder: Retrofit.Builder,
    private val configStore: FabricConfigStore,
) {
    private val fabricRetrofitFactory = RetrofitCachedFactory { it.fabricEndpoint }
    private val authdRetrofitFactory = RetrofitCachedFactory { it.authdEndpoint }
    private val externalWalletFactory = RetrofitCachedFactory { "https://wlt.stg.svc.eluv.io/as" }

    @JvmName("getFabricApi")
    fun <T : FabricApi> getApi(clazz: KClass<T>): Single<T> {
        return fabricRetrofitFactory.get().map { it.create(clazz.java) }
    }

    @JvmName("getAuthdApi")
    fun <T : AuthdApi> getApi(clazz: KClass<T>): Single<T> {
        return authdRetrofitFactory.get().map { it.create(clazz.java) }
    }

    fun <T : AuthdApi> getExternalWalletApi(clazz: KClass<T>): Single<T> {
        return externalWalletFactory.get().map { it.create(clazz.java) }
    }

    // temp place for a way to get the current fabric endpoint
    fun getFabricEndpoint(): Single<String> {
        return fabricRetrofitFactory.get().map { it.baseUrl().toString() }
    }

    private inner class RetrofitCachedFactory(
        private val endpointSelector: (FabricConfiguration) -> String,
    ) {
        // TODO: actually implement failover and don't just create a new instance every time.
        fun get(): Single<Retrofit> {
            return configStore.observeFabricConfiguration()
                .firstOrError()
                .map { config ->
                    // TODO impl failover
                    builder.baseUrl("${endpointSelector(config)}/").build()
                }
        }
    }
}
