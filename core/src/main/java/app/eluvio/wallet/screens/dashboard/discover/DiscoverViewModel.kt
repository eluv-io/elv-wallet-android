package app.eluvio.wallet.screens.dashboard.discover

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.core.BuildConfig
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.DiscoverRowsStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.asNewRoot
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.util.logging.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.PublishProcessor
import javax.inject.Inject
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import app.eluvio.wallet.screens.signin.SignInNavArgs

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val discoverRowsStore: DiscoverRowsStore,
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
        val rows: List<Row> = emptyList(),
        val showRetryButton: Boolean = false,

        // For custom, Property-specific builds only.
        val singlePropertyMode: Boolean = BuildConfig.DEFAULT_PROPERTY_ID != null,
    ) {
        /**
         * Flat list of unique properties, for UIs that don't render categorized rows
         * (single-property mode, mobile grid).
         */
        val properties: List<Property>
            get() = rows.flatMap { it.properties }.distinctBy { it.id }

        @Immutable
        data class Row(
            val title: String,
            val properties: List<Property>,
        )

        @Immutable
        data class Property(
            val id: String,
            val name: String,
            val loginProvider: String,
            val skipLogin: Boolean,

            // For displaying Property-specific branding in the Discover screen.
            val cardImage: FabricUrl?,
            val focusBackgroundUrl: FabricUrl?,
            val logo: FabricUrl?,

            // FAKE (server data model not ready yet, see DiscoverRowsStore).
            val heroVideoUrl: String?,
            val hasWatchProgress: Boolean,

            // For custom, Property-specific builds only.
            val startScreenLogo: FabricUrl?,
            val startScreenBackground: FabricUrl?,
        )
    }

    private val retryTrigger = PublishProcessor.create<Unit>()

    /** The property flow can emit twice (db, then network); only redirect once. */
    private var alreadySkippedStartScreen = false

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
                    copy(loading = true, rows = emptyList(), showRetryButton = false)
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
                    ).map { listOf(State.Row(title = "", properties = listOf(it.toStateProperty()))) }
                } else {
                    discoverRowsStore.observeDiscoverRows(true)
                        .map { rows -> rows.map { it.toStateRow() } }
                }
                    .doOnError {
                        Log.e("Error observing properties ${it.message}, offering retry")
                        fireEvent(Events.NetworkError)
                        updateState { copy(loading = false, showRetryButton = true) }
                    }
                    .onErrorResumeWith(Flowable.never())
            }
            .subscribeBy(
                onNext = { rows ->
                    // Assume that Properties will never be empty once fetched from Server
                    updateState {
                        copy(
                            rows = rows,
                            loading = rows.isEmpty(),
                            showRetryButton = false
                        )
                    }
                    rows.firstOrNull()?.properties?.firstOrNull()
                        ?.let { skipStartScreenIfSignedIn(it) }
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

    /**
     * Single-property builds have no start screen for users who are already signed in — the
     * Property page is the home screen, so there's no "Welcome Back" step to sit through.
     *
     * Set as the new root rather than pushed: the user never chose to be on the start screen, so
     * Back should exit the app instead of returning to it (which would just bounce them here
     * again).
     */
    private fun skipStartScreenIfSignedIn(property: State.Property) {
        if (BuildConfig.DEFAULT_PROPERTY_ID == null || alreadySkippedStartScreen) return
        val loggedInWithSameProvider =
            tokenStore.isLoggedIn && tokenStore.loginProvider.get() == property.loginProvider
        if (loggedInWithSameProvider) {
            alreadySkippedStartScreen = true
            navigateTo(PropertyDetailNavArgs(property.id).asNewRoot())
        }
    }

    fun onPropertyClicked(property: State.Property) {
        val target = PropertyDetailNavArgs(property.id)
        val loggedInWithSameProvider =
            tokenStore.isLoggedIn && tokenStore.loginProvider.get() == property.loginProvider
        if (property.skipLogin || loggedInWithSameProvider) {
            navigateTo(target.asPush())
        } else {
            Log.d("User not signed in, navigating to authFlow and saving propertyId: ${property.id}")
            navigateTo(
                SignInNavArgs(
                    property.loginProvider,
                    property.id,
                    onSignedInTarget = target
                ).asPush()
            )
        }
    }
}

private fun DiscoverRowsStore.Row.toStateRow(): DiscoverViewModel.State.Row {
    return DiscoverViewModel.State.Row(
        title = title,
        properties = items.map { it.property.toStateProperty(it.heroVideoUrl, it.hasWatchProgress) }
    )
}

private fun MediaPropertyEntity.toStateProperty(
    heroVideoUrl: String? = null,
    hasWatchProgress: Boolean = false,
): DiscoverViewModel.State.Property {
    return DiscoverViewModel.State.Property(
        id = id,
        name = name,
        loginProvider = loginProvider,
        skipLogin = loginInfo?.skipLogin == true,

        cardImage = image,
        focusBackgroundUrl = bgImageWithFallback,
        logo = headerLogoUrl,

        heroVideoUrl = heroVideoUrl,
        hasWatchProgress = hasWatchProgress,

        startScreenLogo = startScreenLogo,
        startScreenBackground = startScreenBackground
    )
}
