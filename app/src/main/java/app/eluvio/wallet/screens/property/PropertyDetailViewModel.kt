package app.eluvio.wallet.screens.property

import androidx.lifecycle.SavedStateHandle
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.showAlternatePage
import app.eluvio.wallet.data.entities.v2.permissions.showPurchaseOptions
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PropertySearchStore
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.screens.destinations.PropertySearchDestination
import app.eluvio.wallet.screens.destinations.PurchasePromptDestination
import app.eluvio.wallet.screens.videoplayer.toMediaSource
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.combineLatest
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.mapNotNull
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
    savedStateHandle: SavedStateHandle
) : BaseViewModel<DynamicPageLayoutState>(DynamicPageLayoutState()) {

    private val navArgs = PropertyDetailDestination.argsFrom(savedStateHandle)
    private val propertyId = navArgs.propertyId

    private val unauthorizedPageIds = mutableSetOf<String>()

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
        .switchMap { (property, page) ->
            unauthorizedPageIds.clear()
            getFirstAuthorizedPage(property, page)
                .map { authorizedPage -> property to authorizedPage }
        }
        .switchMap { (property, page) ->
            Flowables.interval(period = 1.minutes, initialDelay = 0.seconds)
                .switchMap {
                    propertyStore.observeSections(property, page, forceRefresh = true)
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
     * Finds the first page we are authorized to view.
     * @throws ShowPurchaseOptionsRedirectException when reaching a property/page that requires
     *  displaying purchase options, since there's no valid [MediaPageEntity] in that case.
     */
    private fun getFirstAuthorizedPage(
        property: MediaPropertyEntity,
        currentPage: MediaPageEntity?
    ): Flowable<MediaPageEntity> {
        // Convenience function to handle redirects
        fun redirect(redirectPageId: String) = propertyStore.observePage(property, redirectPageId)
            .switchMap { nextPage ->
                // Recursively check the next page
                getFirstAuthorizedPage(property, nextPage)
            }

        return if (currentPage == null) {
            // Check property permissions
            val propertyRedirectPage = property.propertyPermissions?.getRedirectPageId()
            if (propertyRedirectPage != null) {
                redirect(propertyRedirectPage)
            } else if (property.propertyPermissions?.showPurchaseOptions == true) {
                Flowable.error(ShowPurchaseOptionsRedirectException(PermissionContext(property.id)))
            } else {
                // We're authorized to view the property, check the main page.
                getFirstAuthorizedPage(property, property.mainPage)
            }
        } else if (currentPage.pagePermissions?.showPurchaseOptions == true) {
            Flowable.error(
                ShowPurchaseOptionsRedirectException(PermissionContext(property.id, currentPage.id))
            )
        } else {
            when (val redirectPageId = currentPage.pagePermissions?.getRedirectPageId()) {
                currentPage.id, in unauthorizedPageIds -> {
                    // We already checked this page id, or this is a self-reference, so we know
                    // we're not authorized to view it.
                    Flowable.error(CircularRedirectException())
                }

                null -> {
                    // No page to redirect to: we are authorized to render this page.
                    Flowable.just(currentPage)
                        .doOnNext { Log.i("Authorized to view page ${currentPage.id}") }
                }

                else -> {
                    Log.w("Reached unauthorized page ${currentPage.id}, redirecting to $redirectPageId")
                    unauthorizedPageIds += currentPage.id
                    redirect(redirectPageId)
                }
            }
        }
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
                section.toDynamicSections(pagePermissionContext, filters)
            }
    }
}

private class CircularRedirectException : IllegalStateException("Circular redirect detected")

private class ShowPurchaseOptionsRedirectException(val permissionContext: PermissionContext) :
    RuntimeException("Show purchase options detected")

/**
 * Video backgrounds are iffy on different devices. We'll want to be smarter about when/if we enable them.
 */
private const val ENABLE_VIDEO_BG = true
