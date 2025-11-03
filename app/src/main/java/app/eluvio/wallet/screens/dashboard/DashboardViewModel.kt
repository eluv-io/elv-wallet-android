package app.eluvio.wallet.screens.dashboard

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.stores.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tokenStore: TokenStore,
) : BaseViewModel<PersistentList<Tabs>>(
    getTabs(tokenStore.isLoggedIn), savedStateHandle
) {
    override fun onResume() {
        super.onResume()

        tokenStore.loggedInObservable
            .subscribeBy { isLoggedIn ->
                updateState { getTabs(isLoggedIn) }
            }
            .addTo(disposables)
    }
}

private fun getTabs(isLoggedIn: Boolean) = if (isLoggedIn) Tabs.AuthTabs else Tabs.NoAuthTabs
