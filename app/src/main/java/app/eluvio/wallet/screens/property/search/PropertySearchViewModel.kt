package app.eluvio.wallet.screens.property.search

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events.ToastMessage
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.SearchFilterAttribute
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PropertySearchStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.network.dto.v2.SearchRequest
import app.eluvio.wallet.screens.destinations.PropertySearchDestination
import app.eluvio.wallet.screens.navArgs
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.toDynamicSections
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.processors.PublishProcessor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PropertySearchViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val searchStore: PropertySearchStore,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PropertySearchViewModel.State>(State(), savedStateHandle) {

    @Immutable
    data class State(
        val loading: Boolean = true,
        val loadingResults: Boolean = false,
        val headerLogo: FabricUrl? = null,
        val propertyName: String? = null,

        val primaryFilters: DynamicPageLayoutState.Section? = null,
        val searchResults: List<DynamicPageLayoutState.Section> = emptyList(),
        val selectedFilters: SelectedFilters? = null,

        val subproperties: List<SubpropertyInfo> = emptyList()
    ) {
        val allSections = listOfNotNull(primaryFilters) + searchResults

        /**
         * Represents the currently selected filters.
         * All instances represent a non-empty selection of at least Primary filter and value.
         */
        data class SelectedFilters(
            val primaryFilterAttribute: SearchFilterAttribute,
            val primaryFilterValue: String,
            /**
             * The secondary filter matching the selected primary filter value.
             * The existence of this field doesn't mean that a secondary filter is selected yet.
             */
            val secondaryFilterAttribute: SearchFilterAttribute?,
            /**
             * When this field is non-null, the secondary filter is actually selected.
             */
            val secondaryFilterValue: String? = null,
        )

        data class SubpropertyInfo(
            val id: String,
            val name: String,
            val logoUrl: String?,
            val selected: Boolean = false,
        )
    }

    private val navArgs = savedStateHandle.navArgs<PropertySearchNavArgs>()
    private val permissionContext = PermissionContext(propertyId = navArgs.propertyId)
    private val property = propertyStore.observeMediaProperty(navArgs.propertyId).asSharedState()

    private val query = BehaviorProcessor.createDefault(QueryUpdate("", true))
    private val manualSearch = PublishProcessor.create<Unit>()

    private val selectedPrimaryFilter =
        BehaviorProcessor.createDefault(Optional.empty<State.SelectedFilters>())

    private val selectedSubpropertyId = BehaviorProcessor.createDefault(Optional.empty<String>())

    /**
     * Once filters are fetched, hold onto them so we can look up attributes/values by ID.
     */
    private var searchFilters: PropertySearchFiltersEntity? = null

    override fun onResume() {
        super.onResume()

        selectedPrimaryFilter
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

        observeSubpropertyInfo()

        searchStore.getFilters(navArgs.propertyId)
            .firstOrError(
                // We don't actually want to observe changes, because in the rare case that filters
                // change WHILE we're already showing them, the user can get into a weird state.
            )
            .subscribeBy(
                onSuccess = { filters ->
                    searchFilters = filters
                    updateState {
                        copy(
                            // Technically we might not have finished loading the Property at this point,
                            // but we still know the filters, so we can show them.
                            loading = false,
                            primaryFilters = filters.primaryFilter?.toCustomCardsSection()
                        )
                    }

                    // Now that everything is ready, we can start observing search triggers.
                    observeSearchTriggers(filters)
                },
                onError = {
                    fireEvent(ToastMessage("We hit a problem. Please try again later."))
                    navigateTo(NavigationEvent.GoBack)
                }
            )
            .addTo(disposables)
    }

    private fun observeSubpropertyInfo() {
        property.map { it.subproperyIds }
            .distinctUntilChanged()
            .flatMap { ids ->
                Flowable.combineLatest(
                    ids.map { id ->
                        propertyStore.observeMediaProperty(id, forceRefresh = false)
                    }
                ) { it.filterIsInstance<MediaPropertyEntity>() }
            }
            .combineLatest(selectedSubpropertyId)
            .map { (properties, selectedSubpropertyId) ->
                properties.map { property ->
                    State.SubpropertyInfo(
                        id = property.id,
                        name = property.name,
                        logoUrl = property.headerLogoUrl?.url,
                        selected = property.id == selectedSubpropertyId.orDefault(null)
                    )
                }
            }
            .subscribeBy {
                println("stav: Subproperties: $it")
                updateState { copy(subproperties = it) }
            }
            .addTo(disposables)
    }

    fun onQueryChanged(query: String) {
        this.query.onNext(QueryUpdate(query, immediate = false))
    }

    fun onBackPressed() {
        val selectedFilter = selectedPrimaryFilter.value?.orDefault(null)
        when {
            query.value?.query?.isNotEmpty() == true -> {
                fireEvent(ResetQueryEvent)
                query.onNext(QueryUpdate("", immediate = true))
            }

            selectedFilter != null -> {
                if (selectedFilter.secondaryFilterValue != null) {
                    // Secondary filter is selected, clear it
                    val updatedFilter = selectedFilter.copy(secondaryFilterValue = null)
                    selectedPrimaryFilter.onNext(Optional.of(updatedFilter))
                } else {
                    // Only primary filter is selected, clear it.
                    selectedPrimaryFilter.onNext(Optional.empty())
                }
            }
            // Using [GoBack] sends us into an infinite loop, so instead we assume we
            // know the current destination, and pop it.
            else -> navigateTo(NavigationEvent.PopTo(PropertySearchDestination, true))
        }
    }

    fun onPrimaryFilterSelected(primaryFilterValue: SearchFilterAttribute.Value?) {
        val selectedFilter = primaryFilterValue?.let {
            val primaryFilterAttribute = searchFilters?.primaryFilter ?: return@let null
            val nextFilter = searchFilters?.attributes?.get(primaryFilterValue.nextFilterAttribute)
            State.SelectedFilters(
                primaryFilterAttribute = primaryFilterAttribute,
                primaryFilterValue = primaryFilterValue.value,
                secondaryFilterAttribute = nextFilter,
            )
        }
        selectedPrimaryFilter.onNext(Optional.of(selectedFilter))
    }

    fun onSecondaryFilterClicked(value: String?) {
        val selectedFilter = selectedPrimaryFilter.value?.orDefault(null)
            ?: return Log.w("Secondary filter selected without primary filter?!")
        val updatedFilter = selectedFilter.copy(
            secondaryFilterValue = value
            // If clicked the currently selected filter, deselect it.
            // Disabled for now, since we switched to select by highlighting.
            // .takeIf { it != selectedFilter.secondaryFilterValue }
        )
        selectedPrimaryFilter.onNext(Optional.of(updatedFilter))
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

    private fun observeSearchTriggers(searchFilters: PropertySearchFiltersEntity) {
        val queryChanged = query
            .switchMapSingle { (query, immediate) ->
                if (immediate) {
                    Single.just(query)
                } else {
                    Single.just(query)
                        .delay(300, TimeUnit.MILLISECONDS)
                }
            }
            .map { SearchTriggers.QueryChanged(it) }
        val searchClicked = manualSearch
            .map { SearchTriggers.QueryChanged(query.value?.query ?: "") }
        val filterChanged = selectedPrimaryFilter
            .doOnNext {
                val primaryFilterSelected = it.orDefault(null)?.primaryFilterValue != null
                updateState {
                    copy(primaryFilters = searchFilters.primaryFilter
                        // only show primary filters while non are selected
                        ?.takeIf { !primaryFilterSelected }
                        ?.toCustomCardsSection())
                }
            }
            .map { SearchTriggers.FilterChanged(it.orDefault(null)) }
            .distinctUntilChanged()

        val subpropertyChanged = selectedSubpropertyId
            .map { SearchTriggers.SubpropertyChanged(it.orDefault(null)) }
            .distinctUntilChanged()

        Flowable.merge(
            queryChanged,
            searchClicked,
            filterChanged,
            subpropertyChanged
        )
            .scan(SearchRequest()) { request, trigger ->
                when (trigger) {
                    is SearchTriggers.QueryChanged -> request.copy(searchTerm = trigger.query)
                    is SearchTriggers.FilterChanged -> {
                        request.copy(attributes = trigger.toAttributeMap())
                    }

                    is SearchTriggers.SubpropertyChanged -> request.copy(subpropertyId = trigger.id)
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
                        section.toDynamicSections(permissionContext)
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

    private fun SearchFilterAttribute.toCustomCardsSection(): DynamicPageLayoutState.Section {
        val propertyContext = permissionContext
        return DynamicPageLayoutState.Section.Carousel(
            permissionContext = propertyContext.copy(sectionId = "PRIMARY_FILTERS"),
            items = values.map { filterValue ->
                DynamicPageLayoutState.CarouselItem.CustomCard(
                    permissionContext = propertyContext.copy(),
                    imageUrl = filterValue.imageUrl,
                    title = filterValue.value,
                    aspectRatio = AspectRatio.WIDE,
                    onClick = { onPrimaryFilterSelected(filterValue) }
                )
            },
            displaySettings = SimpleDisplaySettings(displayFormat = DisplayFormat.GRID),
        )
    }

    fun onSubpropertySelected(subpropertyId: String?) {
        selectedSubpropertyId.onNext(Optional.of(subpropertyId))
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

    data class SubpropertyChanged(val id: String?) : SearchTriggers
}
