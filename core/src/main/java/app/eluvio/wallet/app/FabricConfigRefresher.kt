package app.eluvio.wallet.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.network.dto.FabricConfiguration
import app.eluvio.wallet.util.logging.Log
import io.reactivex.rxjava3.disposables.Disposable
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import kotlinx.coroutines.rx3.rxCompletable
import javax.inject.Inject

/**
 * Keeps an open connection to [FabricConfigStore] which will make it refresh periodically.
 * Also, when the config points us to a new node, update all [FabricUrlEntity]s in the database.
 */
class FabricConfigRefresher @Inject constructor(
    private val fabricConfigStore: FabricConfigStore,
    private val realm: Realm
) : DefaultLifecycleObserver {
    private var disposable: Disposable? = null
    override fun onStart(owner: LifecycleOwner) {
        Log.d("============APP START=========")
        disposable = fabricConfigStore.observeFabricConfiguration()
            .flatMapCompletable { updateAllFabricLinks(it) }
            .retry()
            .subscribe()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d("============APP STOP=========")
        disposable?.dispose()
        disposable = null
    }

    private fun updateAllFabricLinks(config: FabricConfiguration) = rxCompletable {
        realm.write {
            query<FabricUrlEntity>().find().forEach {
                it.updateBaseUrl(config.fabricEndpoint)
            }
        }
    }
}
