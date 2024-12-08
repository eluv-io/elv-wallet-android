package app.eluvio.wallet.screens.property.upcoming

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.permissions.PermissionContextResolver
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.navigation.asReplace
import app.eluvio.wallet.screens.destinations.VideoPlayerActivityDestination
import app.eluvio.wallet.util.realm.millis
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.rx.timer
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.Flowables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class UpcomingVideoViewModel @Inject constructor(
    private val contentStore: ContentStore,
    private val propertyStore: MediaPropertyStore,
    private val apiProvider: ApiProvider,
    private val navArgs: UpcomingVideoNavArgs,
    private val permissionContextResolver: PermissionContextResolver,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<UpcomingVideoViewModel.State>(
    State(mediaItemId = navArgs.mediaItemId, propertyId = navArgs.propertyId),
    savedStateHandle
) {
    data class State(
        val imagesBaseUrl: String? = null,
        val backgroundImageUrl: String? = null,
        val mediaItemId: String = "",
        val propertyId: String = "",
        val title: String = "",
        val icons: List<String> = emptyList(),
        val headers: List<String> = emptyList(),
        val startTimeMillis: Long? = null,
    )

    override fun onResume() {
        super.onResume()

        apiProvider.getFabricEndpoint()
            .subscribeBy { updateState { copy(imagesBaseUrl = it) } }
            .addTo(disposables)

        // This has some duplicated logic from PropertyDetailViewModel, but I'm in a hurry.
        if (navArgs.sourcePageId != null) {
            updateBackgroundImage()
        }
        propertyStore.observeMediaProperty(navArgs.propertyId)
            .subscribeBy {
                updateState { copy(backgroundImageUrl = it.mainPage?.backgroundImageUrl?.url) }
            }
            .addTo(disposables)

        contentStore.observeMediaItem(navArgs.mediaItemId)
            .switchMap { mediaItem ->
                autoNavigateOnStreamStart(mediaItem)
            }
            .subscribeBy(
                onNext = { mediaItem ->
                    updateState {
                        copy(
                            title = mediaItem.name,
                            icons = mediaItem.liveVideoInfo?.icons.orEmpty(),
                            headers = mediaItem.displaySettings?.headers.orEmpty(),
                            startTimeMillis = mediaItem.liveVideoInfo?.eventStartTime?.millis
                        )
                    }
                },
                onError = {}
            )
            .addTo(disposables)
    }

    /**
     * Automatically navigate to the video player when the stream starts.
     * Returns the MediaEntity immediately
     */
    private fun autoNavigateOnStreamStart(mediaItem: MediaEntity): Flowable<MediaEntity> {
        val remainingTime = mediaItem.liveVideoInfo?.streamStartTime
            ?.let { startTime ->
                (startTime.millis - System.currentTimeMillis()).milliseconds
            }
        return if (remainingTime != null && remainingTime.inWholeSeconds > 0) {
            Flowables.timer(remainingTime)
                .doOnNext {
                    navigateTo(
                        VideoPlayerActivityDestination(
                            propertyId = navArgs.propertyId,
                            mediaItemId = navArgs.mediaItemId
                        ).asReplace()
                    )
                }
                // Convert stream to MediaEntity but don't actually emit anything at the end of the timer
                .mapNotNull { null as MediaEntity? }
                .startWithItem(mediaItem)
        } else {
            Flowable.just(mediaItem)
        }
    }

    private fun updateBackgroundImage() {
        // Fake permission context to quickly resolve property and page
        val permissionContext = PermissionContext(
            propertyId = navArgs.propertyId, pageId = navArgs.sourcePageId
        )
        permissionContextResolver.resolve(permissionContext)
            .firstElement()
            .flatMap { (property, page) ->
                propertyStore.observeSections(property, page!!, forceRefresh = false)
                    .firstElement()
                    .mapNotNull { sections ->
                        // Find the first hero section and use its background
                        sections
                            .firstOrNull { it.type == MediaPageSectionEntity.TYPE_HERO }
                            ?.displaySettings
                            ?.heroBackgroundImageUrl
                            ?: page.backgroundImageUrl
                    }
            }
            .subscribeBy {
                updateState { copy(backgroundImageUrl = it.url) }
            }
            .addTo(disposables)
    }
}

/**
 * Returns a user-friendly countdown string, and the amount of reaming seconds until the event starts.
 * If the event has already started, the remaining time will be 0.
 * If the event has no start time, this will return null.
 */
val UpcomingVideoViewModel.State.remainingTimeToStart: Pair<String, Long>?
    get() {
        startTimeMillis ?: return null
        val remainingTime = (startTimeMillis - System.currentTimeMillis()).milliseconds
        val remainingSeconds = remainingTime.inWholeSeconds
        if (remainingSeconds <= 0) {
            // Short circuit a time that has already started
            return "0 Seconds" to 0L
        }

        // Build a pretty string we can show the user.
        val remainingTimeStr = remainingTime.toComponents { days, hours, minutes, seconds, _ ->
            buildString {
                /** Copied and modifier from [Duration.toString] */
                val hasDays = days != 0L
                val hasHours = hours != 0
                val hasMinutes = minutes != 0
                val hasSeconds = seconds != 0
                var components = 0
                if (hasDays) {
                    append(days)
                    if (days == 1L) {
                        append(" Day")
                    } else {
                        append(" Days")
                    }
                    components++
                }
                if (hasHours || (hasDays && (hasMinutes || hasSeconds))) {
                    if (components++ > 0) append(", ")
                    append(hours)
                    if (hours == 1) {
                        append(" Hour")
                    } else {
                        append(" Hours")
                    }
                }
                if (hasMinutes || (hasSeconds && (hasHours || hasDays))) {
                    if (components++ > 0) append(", ")
                    append(minutes)
                    if (minutes == 1) append(" Minute") else
                        append(" Minutes")
                }
                // Always show Seconds
                if (components > 0) append(", ")
                append(seconds)
                if (seconds == 1) {
                    append(" Second")
                } else {
                    append(" Seconds")
                }
            }
        }
        return remainingTimeStr to remainingSeconds
    }
