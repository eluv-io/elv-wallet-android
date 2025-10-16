package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asPush
import com.ramcosta.composedestinations.generated.destinations.PropertyDetailDestination
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.generated.destinations.SignInDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.PublishProcessor
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val tokenStore: TokenStore,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<DiscoverViewModel.State>(
    State(isLoggedIn = tokenStore.isLoggedIn),
    savedStateHandle
) {

    @Immutable
    data class State(
        val loading: Boolean = true,
        val isLoggedIn: Boolean,
        val properties: List<MediaPropertyEntity> = emptyList(),
        val showRetryButton: Boolean = false,

        // For custom, Property-specific builds only.
        val singlePropertyMode: Boolean = BuildConfig.DEFAULT_PROPERTY_ID != null,
    )

    private val retryTrigger = PublishProcessor.create<Unit>()

    override fun onResume() {
        super.onResume()

        tokenStore.loggedInObservable
            .subscribeBy {
                updateState { copy(isLoggedIn = it) }
            }
            .addTo(disposables)

        retryTrigger
            .doOnNext {
                Log.i("Restart triggered, resetting state.")
                updateState {
                    copy(loading = true, properties = emptyList(), showRetryButton = false)
                }
            }
            // Start with a fake "retry" that doesn't affect state, just to start observing data.
            .startWithItem(Unit)
            // restart chain when login state / environment changes
            .combineLatest(tokenStore.loggedInObservable.distinctUntilChanged())
            .switchMap {
                // Restart property observing when log-in state changes
                if (BuildConfig.DEFAULT_PROPERTY_ID != null) {
                    propertyStore.observeMediaProperty(
                        BuildConfig.DEFAULT_PROPERTY_ID,
                        forceRefresh = true
                    ).map { listOf(it) }
                } else {
                    propertyStore.observeDiscoverableProperties(true)
                }
                    .doOnError {
                        Log.e("Error observing properties ${it.message}, offering retry")
                        fireEvent(Events.NetworkError)
                        updateState { copy(loading = false, showRetryButton = true) }
                    }
                    .onErrorResumeWith(Flowable.never())
            }
            .subscribeBy(
                onNext = { properties ->
                    // Assume that Properties will never be empty once fetched from Server
                    updateState {
                        copy(
                            properties = properties,
                            loading = properties.isEmpty(),
                            showRetryButton = false
                        )
                    }
                },
                onError = {
                    Log.e("Reached on onError that should never happen")
                    throw it
                }
            )
            .addTo(disposables)
    }

    fun retry() {
        retryTrigger.onNext(Unit)
    }

    fun onPropertyClicked(property: MediaPropertyEntity) {
        val direction = PropertyDetailDestination(property.id)
        val skipLogin = property.loginInfo?.skipLogin == true
        val loggedInWithSameProvider =
            tokenStore.isLoggedIn && tokenStore.loginProvider.get() == property.loginProvider
        if (skipLogin || loggedInWithSameProvider) {
            navigateTo(direction.asPush())
        } else {
            Log.d("User not signed in, navigating to authFlow and saving propertyId: ${property.id}")
            navigateTo(
                SignInDestination(
                    property.loginProvider,
                    property.id,
                    onSignedInDirection = direction
                ).asPush()
            )
        }
    }
}
