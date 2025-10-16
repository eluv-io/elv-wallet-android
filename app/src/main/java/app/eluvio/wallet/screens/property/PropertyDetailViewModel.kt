package app.eluvio.wallet.screens.property

import androidx.lifecycle.SavedStateHandle
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.showAlternatePage
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.stores.PropertySearchStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.videoplayer.toMediaSource
import app.eluvio.wallet.util.entity.CircularRedirectException
import app.eluvio.wallet.util.entity.ShowPurchaseOptionsRedirectException
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.combineLatest
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.mapNotNull
import com.ramcosta.composedestinations.generated.destinations.PropertyDetailDestination
import com.ramcosta.composedestinations.generated.destinations.PropertySearchDestination
import com.ramcosta.composedestinations.generated.destinations.PurchasePromptDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val propertySearchStore: PropertySearchStore,
    private val videoOptionsFetcher: VideoOptionsFetcher,
    private val playbackStore: PlaybackStore,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<DynamicPageLayoutState>(DynamicPageLayoutState()) {

    private val navArgs = PropertyDetailDestination.argsFrom(savedStateHandle)
    private val propertyId = navArgs.propertyId

    private val property = propertyStore.observeMediaProperty(propertyId)
        .asSharedState()

    private val pageLayout = property
        .switchMap { property ->
            if (navArgs.pageId != null) {
                // Observing a specific page will skip the property permissions check.
                // As long as properties don't link to specific pages in other properties,
                // this should be fine.
                propertyStore.observePage(property, navArgs.pageId)
                    .map { page -> property to page }
            } else {
                // The default case. Not passing mainPage here to make sure we check the
                // Property permissions before we check the page permissions,
                Flowable.just(property to null)
            }
        }
        .switchMapSingle { (property, page) ->
            property.getFirstAuthorizedPage(page, propertyStore)
                .map { authorizedPage -> property to authorizedPage }
        }
        .switchMap { (property, page) ->
            Flowables.interval(period = 1.minutes, initialDelay = 0.seconds)
                .switchMap { tick ->
                    propertyStore.observeSections(property, page, forceRefresh = true)
                        .onErrorResumeNext { error ->
                            if (tick == 0L) {
                                // It's possible we can avoid throwing in this case, if we have a Realm cache,
                                // but we're not checking for that currently.
                                Log.e("Error on first section fetch, throwing error", error)
                                Flowable.error(error)
                            } else {
                                // Ignore errors on subsequent fetches, just wait for the next poll.
                                Log.e("Error on non-first section fetch, ignoring.", error)
                                Flowable.empty()
                            }
                        }
                }
                .map { sections -> sections.associateBy { section -> section.id } }
                .map { sections -> page to sections }
        }
        .asSharedState()

    override fun onResume() {
        super.onResume()

        // Always display Search button.
        updateState { copy(searchNavigationEvent = PropertySearchDestination(propertyId).asPush()) }

        updateSections()

        updateBackground()

        updateSubpropertySelector()
    }

    private fun updateSections() {
        pageLayout.combineLatest(
            propertySearchStore.getFilters(propertyId)
                .onErrorReturnItem(PropertySearchFiltersEntity())
        )
            .subscribeBy(
                onNext = { (page, sections, filters) ->
                    updateState {
                        copy(sections = sections(page, sections, filters))
                    }
                },
                onError = { exception ->
                    when (exception) {
                        is ShowPurchaseOptionsRedirectException -> {
                            Log.e("Show purchase options detected. Navigating.")
                            navigateTo(PurchasePromptDestination(exception.permissionContext).asReplace())
                        }

                        is CircularRedirectException -> {
                            Log.e("Circular redirect detected")
                            fireEvent(Events.ToastMessage("Permission error. Unable to load page."))
                            navigateTo(NavigationEvent.GoBack)
                        }

                        else -> {
                            // Note: might be a problem with deeplinks
                            Log.e("Error loading property detail. Popping screen", exception)
                            fireEvent(Events.NetworkError)
                            navigateTo(NavigationEvent.GoBack)
                        }
                    }
                }
            )
            .addTo(disposables)
    }

    private fun updateSubpropertySelector() {
        val propertyLinks = if (navArgs.propertyLinks.isNotEmpty()) {
            // Property links provided externally. Update the isCurrent flag for each and
            // ignore the Property's subpropertySelection.
            Flowable.just(navArgs.propertyLinks.map { it.copy(isCurrent = it.id == propertyId) })
        } else {
            property.mapNotNull {
                it.subpropertySelection.map { subproperty ->
                    DynamicPageLayoutState.PropertyLink(
                        id = subproperty.id,
                        name = subproperty.title ?: return@mapNotNull null,
                        isCurrent = subproperty.id == propertyId
                    )
                }
            }
        }

        propertyLinks
            // Maintain order, but filter out unauthorized Properties.
            .switchMap { links ->
                // Convert each Link to a flowable that emits the PropertyLink when authorized, or empty Optional when not.
                val optionalLinksFlowable = links
                    .map { propertyLink ->
                        propertyStore.observeMediaProperty(propertyLink.id, forceRefresh = false)
                            .map { property ->
                                // Don't filter out unauthorized Properties yet, we need to emit all properties for
                                // combineLatest to work right. So just emit an empty Optional for unauthorized Properties.
                                Optional.of(propertyLink.takeIf { property.propertyPermissions?.authorized == true })
                            }
                    }
                Flowable.combineLatest(optionalLinksFlowable) { optionalLinks ->
                    optionalLinks.filterIsInstance<Optional<DynamicPageLayoutState.PropertyLink>>()
                        .mapNotNull { it.orDefault(null) }
                }
            }
            .subscribeBy { updateState { copy(propertyLinks = it) } }
            .addTo(disposables)
    }

    /**
     * Updates the state with the background image/video.
     * If a video hash is present, waits for a [MediaSource] to be created before updating the state
     * with a background image, so it won't flicker while video options are being fetched.
     * Background image is still set (if available) as a fallback, for the ui to handle.
     */
    private fun updateBackground() {
        pageLayout
            .mapNotNull { (page, sections) ->
                // Find the first hero section and use its background as the page background.
                val heroSectionSettings = sections.values
                    .firstOrNull { it.type == MediaPageSectionEntity.TYPE_HERO }
                    ?.displaySettings

                // Distill the display settings down to the hero background image or video so
                // .distinctUntilChanged() will emit on changes we actually care about
                SimpleDisplaySettings(
                    heroBackgroundVideoHash = heroSectionSettings?.heroBackgroundVideoHash,
                    heroBackgroundImageUrl = heroSectionSettings?.heroBackgroundImageUrl
                        ?: page.backgroundImageUrl
                )
            }
            .distinctUntilChanged()
            .switchMapSingle { display ->
                Maybe.fromCallable { display.heroBackgroundVideoHash?.takeIf { ENABLE_VIDEO_BG } }
                    .flatMapSingle { hash -> videoOptionsFetcher.fetchVideoOptionsFromHash(hash) }
                    .map { videoEntity -> Optional.of(videoEntity.toMediaSource()) }
                    .defaultIfEmpty(Optional.empty()) // No video hash, no MediaSource
                    .onErrorReturn {
                        Log.e("Error fetching video options", it)
                        Optional.empty()
                    }
                    .map { display.heroBackgroundImageUrl?.url to it.orDefault(null) }
            }
            .subscribeBy(
                onNext = { (bgImageUrl, bgVideo) ->
                    updateState { copy(backgroundImageUrl = bgImageUrl, backgroundVideo = bgVideo) }
                },
                onError = {
                    Log.e("Error fetching video options", it)
                }
            )
            .addTo(disposables)
    }

    /**
     * If we are authorized to view this page/property, or redirect behavior isn't configured, returns null.
     */
    private fun PermissionSettingsEntity.getRedirectPageId(): String? {
        return alternatePageId?.takeIf { showAlternatePage }
    }

    private fun sections(
        page: MediaPageEntity,
        sections: Map<String, MediaPageSectionEntity>,
        filters: PropertySearchFiltersEntity
    ): List<DynamicPageLayoutState.Section> {
        val pagePermissionContext = PermissionContext(
            propertyId = propertyId,
            pageId = page.id
        )
        // We can't just iterate over [sections] because the order of sections is important and it
        // is defined by the Page's sectionIds.
        return page.sectionIds
            .mapNotNull { sections[it] }
            .filterNot { section ->
                section.isHidden
                    .also { if (it) Log.v("Hiding unauthorized section ${section.id}") }
            }
            .flatMap { section ->
                section.toDynamicSections(pagePermissionContext, playbackStore, filters)
            }
    }
}

/**
 * Video backgrounds are iffy on different devices. We'll want to be smarter about when/if we enable them.
 */
private const val ENABLE_VIDEO_BG = true
