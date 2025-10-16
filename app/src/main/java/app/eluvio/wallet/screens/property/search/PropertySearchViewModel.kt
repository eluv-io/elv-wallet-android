package app.eluvio.wallet.screens.property.search

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events.ToastMessage
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.search.FilterValueEntity
import app.eluvio.wallet.data.entities.v2.search.SearchFilter
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.stores.PropertySearchStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.network.dto.v2.SearchRequest
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.toDynamicSections
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.delay
import com.ramcosta.composedestinations.generated.destinations.PropertySearchDestination
import com.ramcosta.composedestinations.generated.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class PropertySearchViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val searchStore: PropertySearchStore,
    private val playbackStore: PlaybackStore,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PropertySearchViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val loading: Boolean = true,
        val loadingResults: Boolean = false,
        val headerLogo: FabricUrl? = null,
        val propertyName: String? = null,

        val searchResults: List<DynamicPageLayoutState.Section> = emptyList(),

        val primaryFilter: SearchFilter? = null,
        val selectedFilters: SelectedFilters? = null,
    ) {
        /**
         * Represents the currently selected filters.
         * All instances represent a non-empty selection of at least Primary filter and value.
         */
        data class SelectedFilters(
            val primaryFilterAttribute: SearchFilter,
            val primaryFilterValue: String,
            /**
             * The secondary filter matching the selected primary filter value.
             * The existence of this field doesn't mean that a secondary filter is selected yet.
             */
            val secondaryFilterAttribute: SearchFilter?,
            /**
             * When this field is non-null, the secondary filter is actually selected.
             */
            val secondaryFilterValue: String? = null,
        )
    }

    private val navArgs = savedStateHandle.navArgs<PropertySearchNavArgs>()
    private val permissionContext = PermissionContext(propertyId = navArgs.propertyId)
    private val property = propertyStore.observeMediaProperty(navArgs.propertyId).asSharedState()

    private val query = BehaviorProcessor.createDefault(QueryUpdate("", true))
    private val manualSearch = PublishProcessor.create<Unit>()

    private val selectedFilter =
        BehaviorProcessor.createDefault(Optional.empty<State.SelectedFilters>())

    override fun onResume() {
        super.onResume()

        selectedFilter
            .subscribeBy {
                updateState { copy(selectedFilters = it.orDefault(null)) }
            }
            .addTo(disposables)

        property
            .subscribeBy {
                updateState {
                    copy(
                        headerLogo = it.headerLogoUrl,
                        propertyName = it.name
                    )
                }
            }
            .addTo(disposables)

        searchStore.getFilters(navArgs.propertyId)
            .firstOrError(
                // We don't actually want to observe changes, because in the rare case that filters
                // change WHILE we're already showing them, the user can get into a weird state.
            )
            .subscribeBy(
                onSuccess = { filters ->
                    val primaryFilter = filters.buildPrimaryFilter()
                    if (primaryFilter != null) {
                        primaryFilter.values
                            // When filters contain an "All" option, it should be selected by default
                            .firstOrNull { it.value == FilterValueEntity.ALL }
                            ?.let { value -> State.SelectedFilters(primaryFilter, value.value, value.nextFilter) }
                            ?.let { selectedFilter.onNext(Optional.of(it)) }
                    }
                    updateState {
                        copy(
                            // Technically we might not have finished loading the Property at this point,
                            // but we still know the filters, so we can show them.
                            loading = false,
                            primaryFilter = primaryFilter,
                        )
                    }

                    // Now that everything is ready, we can start observing search triggers.
                    observeSearchTriggers()
                },
                onError = {
                    fireEvent(ToastMessage("We hit a problem. Please try again later."))
                    navigateTo(NavigationEvent.GoBack)
                }
            )
            .addTo(disposables)
    }

    fun onQueryChanged(query: String) {
        this.query.onNext(QueryUpdate(query, immediate = false))
    }

    fun onBackPressed() {
        val selectedFilter = selectedFilter.value?.orDefault(null)
        when {
            query.value?.query?.isNotEmpty() == true -> {
                fireEvent(ResetQueryEvent)
                query.onNext(QueryUpdate("", immediate = true))
            }

            selectedFilter != null -> {
                if (selectedFilter.secondaryFilterValue != null) {
                    // Secondary filter is selected, clear it
                    val updatedFilter = selectedFilter.copy(secondaryFilterValue = null)
                    this.selectedFilter.onNext(Optional.of(updatedFilter))
                } else {
                    // Only primary filter is selected, clear it.
                    this.selectedFilter.onNext(Optional.empty())
                }
            }
            // Using [GoBack] sends us into an infinite loop, so instead we assume we
            // know the current destination, and pop it.
            else -> navigateTo(NavigationEvent.PopTo(PropertySearchDestination, true))
        }
    }

    fun onPrimaryFilterClick(primaryFilterValue: SearchFilter.Value) {
        val currentSelection = selectedFilter.value?.orDefault(null)
        if (currentSelection?.primaryFilterValue == primaryFilterValue.value) {
            // If clicked the currently selected filter, deselect it.
            selectedFilter.onNext(Optional.empty())
        } else {
            val nextFilter = primaryFilterValue.nextFilter
            val newSelection = State.SelectedFilters(
                primaryFilterAttribute = primaryFilterValue.parent,
                primaryFilterValue = primaryFilterValue.value,
                secondaryFilterAttribute = nextFilter,
            )
            selectedFilter.onNext(Optional.of(newSelection))
        }
    }

    fun onSecondaryFilterClick(secondaryFilterValue: SearchFilter.Value) {
        val selectedFilter = selectedFilter.value?.orDefault(null)
            ?: return Log.w("Secondary filter selected without primary filter?!")
        val updatedFilter = selectedFilter.copy(
            secondaryFilterValue = secondaryFilterValue.value
                // If clicked the currently selected filter, deselect it.
                .takeIf { it != selectedFilter.secondaryFilterValue }
        )
        this.selectedFilter.onNext(Optional.of(updatedFilter))
    }

    fun onSearchClicked() {
        manualSearch.onNext(Unit)
    }

    private fun fetchResults(request: SearchRequest): Single<List<MediaPageSectionEntity>> {
        val propertyToSearch = if (request.subpropertyId != null) {
            propertyStore.observeMediaProperty(request.subpropertyId, forceRefresh = false)
        } else {
            property
        }
        return propertyToSearch.firstOrError()
            .flatMap { property ->
                searchStore.search(property, request)
            }
            .doOnSubscribe {
                updateState { copy(loadingResults = true) }
                Log.d("Starting to search for $request")
            }
            .doFinally {
                updateState { copy(loadingResults = false) }
                Log.d("Done searching for $request")
            }
    }

    private fun observeSearchTriggers() {
        val queryChanged = query
            .switchMapSingle { (query, immediate) ->
                if (immediate) {
                    Single.just(query)
                } else {
                    Single.just(query).delay(300.milliseconds)
                }
            }
            .map { SearchTriggers.QueryChanged(it) }
        val searchClicked = manualSearch
            .map { SearchTriggers.QueryChanged(query.value?.query ?: "") }
        val filterChanged = selectedFilter
            .map { SearchTriggers.FilterChanged(it.orDefault(null)) }
            .distinctUntilChanged()

        Flowable.merge(
            queryChanged,
            searchClicked,
            filterChanged,
        )
            .scan(SearchRequest()) { request, trigger ->
                when (trigger) {
                    is SearchTriggers.QueryChanged -> request.copy(searchTerm = trigger.query)
                    is SearchTriggers.FilterChanged -> {
                        request.copy(attributes = trigger.toAttributeMap())
                    }
                }
            }
            .distinctUntilChanged()
            .switchMapMaybe { request ->
                fetchResults(request)
                    .doOnError { error ->
                        Log.d("Error during search", error)
                        updateState { copy(searchResults = messageResult("Encountered an error. Please try again later.")) }
                    }
                    // Consume errors, so that the stream doesn't die.
                    .onErrorComplete()
            }
            .subscribeBy { results ->
                updateState {
                    val sections = results.flatMap { section ->
                        section.toDynamicSections(permissionContext, playbackStore)
                    }.ifEmpty {
                        // Show a message when no results are found
                        messageResult("No results found")
                    }
                    copy(searchResults = sections)
                }
            }
            .addTo(disposables)
    }

    private fun messageResult(message: String): List<DynamicPageLayoutState.Section.Title> {
        return listOf(
            DynamicPageLayoutState.Section.Title(
                "message_result",
                AnnotatedString(message)
            )
        )
    }
}

private data class QueryUpdate(val query: String, val immediate: Boolean)

private sealed interface SearchTriggers {
    data class QueryChanged(val query: String) : SearchTriggers

    data class FilterChanged(
        val filter: PropertySearchViewModel.State.SelectedFilters?
    ) : SearchTriggers {
        fun toAttributeMap(): Map<String, List<String>>? = filter?.run {
            buildMap {
                put(primaryFilterAttribute.id, listOf(primaryFilterValue))
                if (secondaryFilterAttribute != null && secondaryFilterValue != null) {
                    put(
                        secondaryFilterAttribute.id,
                        listOf(secondaryFilterValue)
                    )
                }
            }
        }
    }
}
