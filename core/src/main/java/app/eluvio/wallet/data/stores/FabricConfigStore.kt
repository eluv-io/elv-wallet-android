package app.eluvio.wallet.data.stores

import app.eluvio.wallet.app.FabricConfigRefresher
import app.eluvio.wallet.network.api.FabricConfigApi
import app.eluvio.wallet.network.dto.FabricConfiguration
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.timeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes


@Singleton
class FabricConfigStore @Inject constructor(
    private val environmentStore: EnvironmentStore,
    private val fabricConfigApi: FabricConfigApi,
) {
    private val configFetchInterval = 3.minutes

    /**
     * Effectively an in-memory cache of the config, that will self-refresh as long as someone is
     * subscribed (that someone is the [FabricConfigRefresher], which runs as long as the app does.
     * Also keeps track of which environment the current config is for.
     */
    private val config: Flowable<Pair<Environment, FabricConfiguration>> =
        environmentStore.observeSelectedEnvironment()
            .switchMapSingle { currentEnv ->
                fabricConfigApi.getConfig(currentEnv.configUrl)
                    .doOnSuccess {
                        Log.d("fetched Config for Env($currentEnv): $it")
                    }
                    .map { config -> currentEnv to config }
            }
            // As long as someone is subscribed, this will reset every [configFetchInterval]
            .doOnError { Log.e("Error fetching config:", it) }
            .retryWhen {
                // retry with delay on actual errors
                //TODO: Make this retry with backoff
                it.delay(5, TimeUnit.SECONDS)
            }
            .timeout(configFetchInterval)
            // retry immediately on fetch interval "error"
            .retry()
            .asSharedState()

    fun observeFabricConfiguration(): Flowable<FabricConfiguration> =
        config.map { (_, config) -> config }

    fun setEnvAndAwaitNewConfig(newEnv: Environment): Completable {
        return config
            .takeWhile { (currentEnv, _) ->
                // Keep going while current config is not for the selected env.
                currentEnv != newEnv
            }
            .doOnNext {
                Log.v("Switching Environment to $newEnv. Waiting for new config...")
                environmentStore.setSelectedEnvironment(newEnv)
            }
            .ignoreElements()
            .doOnComplete {
                Log.i("Environment switched to $newEnv and new config is cached")
            }
    }
}
