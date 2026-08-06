package app.eluvio.wallet.screens.property

import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.PropertyLink
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.display.resolveCardTheme
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.core.BuildConfig
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.stores.PropertySearchStore
import app.eluvio.wallet.screens.dashboard.myitems.MyItemsNavArgs
import app.eluvio.wallet.screens.dashboard.profile.ProfileNavArgs
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.util.entity.CircularRedirectException
import app.eluvio.wallet.util.entity.ShowPurchaseOptionsRedirectException
import app.eluvio.wallet.util.entity.getFirstAuthorizedPage
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.Optional
import app.eluvio.wallet.util.rx.asSharedState
import app.eluvio.wallet.util.rx.combineLatest
import app.eluvio.wallet.util.rx.interval
import app.eluvio.wallet.util.rx.mapNotNull
import com.stavfx.nav3hiltvm.annotations.HiltNavArgViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import app.eluvio.wallet.screens.property.search.PropertySearchNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs

@HiltNavArgViewModel
open class PropertyDetailViewModel(
    @NavArg private val navArgs: PropertyDetailNavArgs,
    private val propertyStore: MediaPropertyStore,
    private val propertySearchStore: PropertySearchStore,
    private val videoOptionsFetcher: VideoOptionsFetcher,
    private val playbackStore: PlaybackStore,
    private val contentStore: ContentStore,
) : BaseViewModel<DynamicPageLayoutState>(DynamicPageLayoutState()) {

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
                .map { sections -> Triple(property, page, sections) }
        }
        .asSharedState()

    override fun onResume() {
        super.onResume()

        // Always display Search button.
        updateState { copy(searchNavigationEvent = PropertySearchNavArgs(propertyId).asPush()) }

        showDashboardTabsInSinglePropertyMode()

        updateSections()

        updateBackground()

        updateSubpropertySelector()

        property
            .subscribeBy { updateState { copy(headerLogo = it.headerLogoUrl) } }
            .addTo(disposables)
    }

    /**
     * Single-property builds skip the Dashboard entirely, so its drawer isn't around to reach
     * Profile/My Items from — the Property page's action row hosts them instead.
     *
     * My Items only appears once we know the user actually owns something here, mirroring the
     * fetch the My Items screen itself performs.
     */
    private fun showDashboardTabsInSinglePropertyMode() {
        if (BuildConfig.DEFAULT_PROPERTY_ID == null) return

        updateState { copy(profileNavigationEvent = ProfileNavArgs.asPush()) }

        contentStore.search(propertyId, displayName = null)
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
            .subscribeBy(
                onNext = { ownsItems ->
                    updateState {
                        copy(myItemsNavigationEvent = MyItemsNavArgs.asPush().takeIf { ownsItems })
                    }
                },
                onError = {
                    // Not fatal - just means we can't offer My Items from here.
                    Log.w("Failed to check for owned items: ${it.message}")
                }
            )
            .addTo(disposables)
    }

    private fun updateSections() {
        Flowables.combineLatest(
            pageLayout,
            propertySearchStore.getFilters(propertyId)
                .onErrorReturnItem(PropertySearchFiltersEntity())
        )
            .switchMap { (layout, filters) ->
                val (property, page, sections) = layout
                heroActionMedia(sections.values)
                    .map { heroMedia -> sections(property, page, sections, filters, heroMedia) }
            }
            .subscribeBy(
                onNext = { newSections ->
                    updateState {
                        copy(sections = newSections)
                    }
                },
                onError = { exception ->
                    when (exception) {
                        is ShowPurchaseOptionsRedirectException -> {
                            Log.e("Show purchase options detected. Navigating.")
                            navigateTo(
                                PurchasePromptNavArgs(exception.permissionContext).asReplace()
                            )
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
                val subpropertyLinks = it.subpropertySelection.mapNotNull { subproperty ->
                    PropertyLink(
                        id = subproperty.id,
                        name = subproperty.title ?: return@mapNotNull null,
                        isCurrent = subproperty.id == propertyId
                    )
                }
                val thisProperty = PropertyLink(
                    id = it.id,
                    name = it.name,
                    isCurrent = it.id == propertyId
                )
                listOf(thisProperty) + subpropertyLinks
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
                    optionalLinks.filterIsInstance<Optional<PropertyLink>>()
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
            .mapNotNull { (_, page, sections) ->
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
                    .map { playoutInfo -> Optional.of(ImmutableMediaSource(playoutInfo.mediaSource)) }
                    .defaultIfEmpty(Optional.empty()) // No video hash, no MediaSource
                    .onErrorReturn {
                        Log.e("Error fetching video options", it)
                        Optional.empty()
                    }
                    .map { display.heroBackgroundImageUrl to it.orDefault(null) }
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
     * Hero actions only tell us the id of the media they link to, so we have to fetch those media
     * items ourselves before we can figure out where a hero button should navigate to.
     * Starts off empty, so the rest of the page doesn't have to wait for this to complete.
     */
    private fun heroActionMedia(
        sections: Collection<MediaPageSectionEntity>
    ): Flowable<Map<String, MediaEntity>> {
        val mediaIds = sections
            .flatMap { it.items + it.subSections.flatMap { subSection -> subSection.items } }
            .flatMap { it.actions }
            .mapNotNull { it.mediaId }
            .distinct()
        if (mediaIds.isEmpty()) {
            return Flowable.just(emptyMap())
        }
        return contentStore.observeMediaItems(propertyId, mediaIds, forceRefresh = false)
            .map { mediaItems -> mediaItems.associateBy { it.id } }
            .onErrorReturn {
                Log.e("Error fetching media for hero actions", it)
                emptyMap()
            }
            .startWithItem(emptyMap())
    }

    private fun sections(
        property: MediaPropertyEntity,
        page: MediaPageEntity,
        sections: Map<String, MediaPageSectionEntity>,
        filters: PropertySearchFiltersEntity,
        heroActionMedia: Map<String, MediaEntity>,
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
                section.toDynamicSections(
                    pagePermissionContext,
                    playbackStore,
                    filters,
                    heroActionMedia
                ) { property.resolveCardTheme(page, it) }
            }
    }
}

/**
 * Video backgrounds are iffy on different devices. We'll want to be smarter about when/if we enable them.
 */
private const val ENABLE_VIDEO_BG = true
