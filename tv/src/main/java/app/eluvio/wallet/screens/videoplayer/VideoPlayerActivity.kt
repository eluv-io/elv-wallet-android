package app.eluvio.wallet.screens.videoplayer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import app.eluvio.wallet.R
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.VideoPlayoutInfo
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.onClickTarget
import app.eluvio.wallet.screens.property.upcoming.UpcomingVideoNavArgs
import app.eluvio.wallet.screens.purchaseprompt.PurchasePromptNavArgs
import app.eluvio.wallet.screens.videoplayer.ui.ScrubThumbnailView
import app.eluvio.wallet.screens.videoplayer.ui.StreamSelectionPane
import app.eluvio.wallet.screens.videoplayer.ui.UpNextPane
import app.eluvio.wallet.screens.videoplayer.ui.VideoInfoPane
import app.eluvio.wallet.util.crypto.Base58
import app.eluvio.wallet.util.exoplayer.defaultSeekPositionMs
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.media.ThumbnailLoader
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.rx.safeDispose
import app.eluvio.wallet.util.sha256
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.muxstats.monitorWithMuxData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.media3.ui.R as media3R

@AndroidEntryPoint
@UnstableApi
class VideoPlayerActivity : FragmentActivity(), Player.Listener {
    @Inject
    lateinit var videoOptionsFetcher: VideoOptionsFetcher

    @Inject
    lateinit var playbackStore: PlaybackStore

    @Inject
    lateinit var propertyStore: MediaPropertyStore

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var envStore: EnvironmentStore

    @Inject
    lateinit var thumbnailLoader: ThumbnailLoader

    @Inject
    lateinit var streamSelectionLoader: StreamSelectionLoader

    @Inject
    lateinit var upNextLoader: UpNextLoader

    private var disposables = CompositeDisposable()

    private var playerView: PlayerView? = null
    private var exoPlayer: ExoPlayer? = null

    private var playPauseButton: View? = null
    private var timeBar: DefaultTimeBar? = null
    private var scrubThumbnailView: ScrubThumbnailView? = null

    private var titleView: TextView? = null
    private var liveIndicator: View? = null
    private var infoButton: View? = null
    private var infoPane: VideoInfoPane? = null
    private var streamsButton: View? = null
    private var streamSelectionPane: StreamSelectionPane? = null
    private var availableStreams: List<StreamItem> = emptyList()

    private var upNextPane: UpNextPane? = null

    /** The answer to "what plays after this?", fetched while the video is still running. */
    private var upNextItem: MediaEntity? = null

    /** The item the card is currently offering. */
    private var offeredUpNextItem: MediaEntity? = null

    /** Set once playback reaches the end, so a late answer still puts the card up. */
    private var upNextRequested = false

    /** The viewer declined this ending. Watching up to it again earns a fresh offer. */
    private var upNextDeclined = false

    private var upNextDisposable: Disposable? = null
    private var upNextPrefetchMessage: PlayerMessage? = null

    // List of buttons that aren't handled by exoplayer, and we need to handle manually.
    private val customControllerButtons: List<View>
        get() = listOfNotNull(liveIndicator, infoButton, streamsButton)

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // No need to propagate event.
            // This callback should only be enabled when the controller is visible.
            playerView?.hideController()
        }
    }

    private val scrubListener = object : TimeBar.OnScrubListener {
        override fun onScrubStart(timeBar: TimeBar, position: Long) {
            positionThumbnailAboveTimeBar()
            updateScrubThumbnailXPosition(position)
        }

        override fun onScrubMove(timeBar: TimeBar, position: Long) {
            updateScrubThumbnailXPosition(position)
        }

        override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            scrubThumbnailView?.hide()
        }

        private fun updateScrubThumbnailXPosition(position: Long) {
            val duration = exoPlayer?.duration ?: return
            if (duration <= 0) return

            val fraction = position.toFloat() / duration.toFloat()
            scrubThumbnailView?.updatePosition(position, fraction)
        }
    }

    // This is for deeplink demo only.
    private var fakeMediaItemId: String? = null

    @Inject
    lateinit var contentStore: ContentStore

    private val navArgs by lazy {
        Json.decodeFromString(
            VideoPlayerArgs.serializer(),
            intent.getStringExtra(VIDEO_PLAYER_ARGS_EXTRA)
                ?: error("VideoPlayerActivity launched without args"),
        )
    }

    // This holds the "original" media id the player was launched for, but might not reflect the
    // currently playing/selected media item, since the stream selector allows us to switch to
    // another Media Item or another fabric video object.
    private val mediaItemId by lazy { fakeMediaItemId ?: navArgs.mediaItemId }
    private var currentlyPlayingStreamId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        playPauseButton = findViewById(media3R.id.exo_play_pause)
        timeBar = findViewById(media3R.id.exo_progress)
        timeBar?.setKeyTimeIncrement(5000)

        titleView = findViewById<TextView>(R.id.video_title)?.apply {
            text = navArgs.mediaTitle
        }

        liveIndicator = findViewById(R.id.live_indicator)
        liveIndicator?.setOnClickListener {
            exoPlayer?.seekToDefaultPosition()
            exoPlayer?.playWhenReady = true
            playPauseButton?.requestFocus()
        }

        infoPane = findViewById(R.id.video_player_info_pane)
        infoPane?.setOnRestartClickListener {
            exoPlayer?.seekTo(0)
            exoPlayer?.playWhenReady = true
            infoPane?.isVisible = false
        }
        infoButton = findViewById(R.id.video_player_info_button)
        infoButton?.setOnClickListener { showInfoPane() }

        streamsButton = findViewById(R.id.video_player_streams_button)
        streamsButton?.setOnClickListener { showStreamSelectionPane() }

        streamSelectionPane = findViewById(R.id.video_player_stream_selection_pane)
        streamSelectionPane?.setOnStreamSelectedListener { stream ->
            switchToStream(stream)
        }

        upNextPane = findViewById<UpNextPane>(R.id.video_player_up_next_pane)?.apply {
            onCancel = { declineUpNext() }
            onPlay = { offeredUpNextItem?.let { playUpNext(it) } }
        }

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            .build()
            .apply {
                addListener(this@VideoPlayerActivity)
            }
        playerView = findViewById<PlayerView>(R.id.video_player_view)?.apply {
            setShowSubtitleButton(true)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            controllerHideOnTouch = false
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                backPressedCallback.isEnabled = visibility == View.VISIBLE
                Log.v("Controller visibility changed: $visibility. Handling back press: ${backPressedCallback.isEnabled}")
                if (visibility == View.VISIBLE) {
                    infoPane?.isVisible = false
                }
            })
            player = exoPlayer

            // Manually show spinner until exoplayer figures itself out
            //noinspection MissingInflatedId
            findViewById<View>(media3R.id.exo_buffering).visibility = View.VISIBLE
        }

        scrubThumbnailView = findViewById(R.id.scrub_thumbnail_view)

        //noinspection MissingInflatedId
        timeBar = findViewById<DefaultTimeBar>(media3R.id.exo_progress)?.apply {
            setKeyTimeIncrement(5000)
            addListener(scrubListener)
        }


        Maybe.fromCallable { navArgs.deeplinkhack_contract }
            .flatMap { base58contract ->
                // Assume we own a token for this contract. If we don't, we'll just be stuck loading forever.
                contentStore.observeWalletData()
                    .mapNotNull { result ->
                        val contract = Base58.decodeAsHex(base58contract.removePrefix("ictr"))
                        result.getOrNull()
                            ?.find { nft ->
                                nft.contractAddress.contains(
                                    contract,
                                    ignoreCase = true
                                )
                            }
                            ?.featuredMedia
                            ?.firstOrNull { it.mediaType == MediaEntity.MEDIA_TYPE_VIDEO }
                            ?.id
                    }
                    .firstElement()
            }
            .doOnSuccess {
                Log.w("Found media item id from deeplink hack: $it")
                fakeMediaItemId = it
            }
            .ignoreElement()
            .subscribeBy(
                onComplete = {
                    currentlyPlayingStreamId = mediaItemId
                    loadVideoFromMediaItem(mediaItemId)
                    loadMetadata(mediaItemId)
                    loadStreamSelections(mediaItemId)
                },
                onError = {
                    Log.e("VideoPlayerFragment: Error fetching video options", it)
                    Toast.makeText(
                        this,
                        "Error loading video. Try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
            .addTo(disposables)
    }

    private fun loadVideoFromMediaItem(mediaItemId: String) {
        loadVideo(
            videoOptionsFetcher.fetchVideoOptions(mediaItemId, navArgs.propertyId),
            mediaItemId
        )
    }

    private fun loadVideoFromHash(hash: String, streamId: String) {
        loadVideo(videoOptionsFetcher.fetchVideoOptionsFromHash(hash), streamId)
    }

    private fun loadVideo(videoOptions: Single<VideoPlayoutInfo>, streamId: String) {
        // this is all we really need if it wasn't for all the fake stuff
        Singles.zip(
            videoOptions,
            propertyStore
                .observeMediaProperty(navArgs.propertyId ?: "", forceRefresh = false)
                .firstOrError(),
            envStore.observeSelectedEnvironment().firstOrError()
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (playoutInfo, property, env) ->
                    playoutInfo.mediaSource.mediaItem.localConfiguration?.uri
                        ?.toString()
                        ?.let { uri ->
                            val customerData = createCustomerData(property, uri)
                            exoPlayer?.monitorWithMuxData(
                                this@VideoPlayerActivity,
                                env.muxEnvKey,
                                customerData,
                                playerView
                            )

                            // Load thumbnails for scrubbing if available
                            playoutInfo.thumbnailsWebVttUrl?.let { thumbnailsUrl ->
                                loadThumbnails(thumbnailsUrl)
                            }
                        }
                    exoPlayer?.setMediaSource(playoutInfo.mediaSource)
                    exoPlayer?.playWhenReady = true
                    exoPlayer?.prepare()
                    exoPlayer?.seekTo(playbackStore.getPlaybackPosition(streamId))
                },
                onError = {
                    Log.e("VideoPlayerFragment: Error fetching video options", it)
                    Toast.makeText(
                        this,
                        "Error loading video. Try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            )
            .addTo(disposables)
    }

    private fun objectIdFromHash(versionHash: String?): String? {
        if (versionHash == null || !versionHash.startsWith("hq__") && !versionHash.startsWith("tq__")) {
            return null
        }
        val bytes = Base58.decode(versionHash.substringAfter("q__"))
            // First 32 bytes are the "digest" and we don't need it.
            .drop(32)
            .dropWhile {
                // Next is the "size", skip that too
                it < 0
            }
            // The next byte is also part of "size".
            .drop(1)
            .toByteArray()

        return "iq__${Base58.encode(bytes)}"
    }

    private fun createCustomerData(property: MediaPropertyEntity, videoUri: String) =
        CustomerData().apply {
            val uri = videoUri.toUri()
            val pathParts = uri.pathSegments
            val versionHash = pathParts.find { it.startsWith("hq__") }
            val offering = pathParts.indexOf("rep")
                .takeIf { it != -1 }
                ?.let { repIndex ->
                    // The "offering" can appear as rep/playout/{offering} or rep/channel/{offering}.
                    // Either way, it's two parts after "rep".
                    pathParts[repIndex + 2]
                }

            customerPlayerData = CustomerPlayerData().apply {
                playerName = "Android-ExoPlayer"
                subPropertyId = property.tenantId
                viewerUserId = tokenStore.walletAddress.get()?.sha256
            }
            customerVideoData = CustomerVideoData().apply {
                videoId = objectIdFromHash(versionHash)
                videoVariantId = versionHash
                videoVariantName = offering
                videoTitle = navArgs.mediaTitle
                videoCdn = uri.host
            }
            customerViewData = CustomerViewData().apply {
                viewSessionId = uri.getQueryParameter("sid")
            }
        }

    private fun loadMetadata(mediaItemId: String) {
        contentStore.observeMediaItem(mediaItemId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                titleView?.text = it.name
                infoPane?.setDisplaySettings(it.requireDisplaySettings())
            }
            .addTo(disposables)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        // re-calc live state on every event
        updateLiveTagState()
    }

    private fun updateLiveTagState() {
        val exoPlayer = exoPlayer ?: return
        val isLive = exoPlayer.isCurrentMediaItemLive
        liveIndicator?.isVisible = isLive
        infoPane?.setRestartButtonEnabled(!isLive)
        if (isLive) {
            /**
             * [ExoPlayer.getCurrentLiveOffset] usually returns C.TIME_UNSET, so instead we check
             * proximity to:
             * 1) The duration, or "end" position of the stream.
             * 2) The "default" position. This is when the player will try to be when playing live
             *    content. This can be ~30 seconds behind the "end" position of the stream.
             */
            val isCloseToLiveEdge = with(exoPlayer) {
                val isCloseToDefaultPosition =
                    abs(defaultSeekPositionMs - contentPosition) < LIVE_EDGE_PROXIMITY_THRESHOLD
                val isCloseToEnd = contentDuration - contentPosition < LIVE_EDGE_PROXIMITY_THRESHOLD
                isCloseToDefaultPosition || isCloseToEnd
            }
            val playingLive = exoPlayer.isPlaying && isCloseToLiveEdge
            // Setting the Activated state will make the label turn red.
            liveIndicator?.isActivated = playingLive
            liveIndicator?.isFocusable = !playingLive
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> scheduleUpNextPrefetch()
            // An empty player reports itself as ended too, and [onResume] prepares one before the
            // playout request comes back. Only a real ending is worth offering the next item for.
            Player.STATE_ENDED -> if (exoPlayer?.currentMediaItem != null) offerUpNext()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e("Error playing video ${error.errorCodeName}")
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            // Re-initialize player at the live edge.
            exoPlayer?.seekToDefaultPosition()
            exoPlayer?.prepare()
        } else {
            // Handle other errors
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("onResume")
        playerView?.onResume()
        exoPlayer?.prepare()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.let { exoPlayer ->
            val currentPosition = exoPlayer.currentPosition
            if (shouldStorePlaybackPosition(currentPosition)) {
                Log.d("Saving playback position $currentPosition")
                playbackStore.setPlaybackPosition(
                    currentlyPlayingStreamId,
                    currentPosition,
                    exoPlayer.contentDuration
                )
            } else {
                playbackStore.setPlaybackPosition(
                    currentlyPlayingStreamId,
                    0,
                    exoPlayer.contentDuration
                )
            }
        }
        playerView?.onPause()
        // Stop player to release resources, instead of just pausing.
        playerView?.player?.stop()
    }

    override fun onDestroy() {
        Log.d("onDestroy")
        upNextPrefetchMessage?.cancel()
        upNextPrefetchMessage = null
        upNextPane = null
        playerView = null
        exoPlayer?.release()
        exoPlayer = null
        disposables.safeDispose()
        disposables = CompositeDisposable()
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (upNextPane?.isVisible == true) {
            // Back declines the offer, same as pressing Cancel.
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) declineUpNext()
                return true
            }
            return upNextPane?.dispatchKeyEvent(event) == true
        }
        if (infoPane?.isVisible == true) {
            Log.d("Forwarding key event to info pane")
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                infoPane?.visibility = View.GONE
                playerView?.showController()
                return true
            }
            return infoPane?.dispatchKeyEvent(event) == true
        }
        if (streamSelectionPane?.isVisible == true) {
            Log.d("Forwarding key event to stream selection pane")
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                streamSelectionPane?.visibility = View.GONE
                playerView?.controllerAutoShow = true
                playerView?.showController()
                return true
            }
            return streamSelectionPane?.dispatchKeyEvent(event) == true
        }
        // Capture ENTER key events on custom buttons
        if (event.keyCode in listOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER)) {
            if (currentFocus in customControllerButtons) {
                Log.d("Non-exoplayer button focused, forwarding key event to view")
                return currentFocus?.dispatchKeyEvent(event) == true
            }
        }
        return playerView?.dispatchKeyEvent(event) == true || super.dispatchKeyEvent(event)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * If the current position is close enough to the start or end of the video, don't bother storing it.
     */
    private fun shouldStorePlaybackPosition(currentPosition: Long): Boolean {
        val position = currentPosition.milliseconds
        val startThreshold = 5.seconds
        if (position < startThreshold) {
            // Too close to the start, don't bother storing
            return false
        }
        val duration = (exoPlayer?.duration ?: 0).milliseconds
        val endThreshold = 15.seconds
        return duration - position > endThreshold
    }

    private fun showInfoPane() {
        playerView?.hideController()
        infoPane?.animateShow()
    }

    private fun loadThumbnails(thumbnailsUrl: String) {
        thumbnailLoader.loadThumbnails(thumbnailsUrl)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { sprite ->
                    Log.d("Loaded ${sprite.cues.size} thumbnail cues")
                    scrubThumbnailView?.setThumbnailSprite(sprite)
                },
                onError = { error ->
                    Log.w("Failed to load thumbnails", error)
                    // Thumbnails are optional, don't show error to user
                }
            )
            .addTo(disposables)
    }

    private fun loadStreamSelections(mediaItemId: String) {
        streamSelectionLoader.getStreams(mediaItemId, navArgs.propertyId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { streams ->
                    // streams always includes the current mediaItem as first element.
                    // Show selector only if there are other streams beyond the current item.
                    if (streams.size <= 1) {
                        Log.d("No stream selections or only one stream available")
                    } else {
                        Log.d("Loaded ${streams.size} stream selections")
                        availableStreams = streams
                        streamsButton?.isVisible = true
                        streamSelectionPane?.setCurrentlyPlayingStreamId(currentlyPlayingStreamId)
                        streamSelectionPane?.setStreams(availableStreams)
                    }
                },
                onError = { error ->
                    Log.w("Failed to load stream selections", error)
                    // Stream selection is optional, don't show error to user
                }
            )
            .addTo(disposables)
    }

    private fun showStreamSelectionPane() {
        playerView?.hideController()
        playerView?.controllerAutoShow = false
        streamSelectionPane?.setCurrentlyPlayingStreamId(currentlyPlayingStreamId)
        streamSelectionPane?.animateShow()
    }

    private fun switchToStream(stream: StreamItem) {
        streamSelectionPane?.visibility = View.GONE
        playerView?.controllerAutoShow = true
        titleView?.text = stream.title
        resetUpNext()

        when (stream) {
            is StreamItem.MediaItem -> {
                fakeMediaItemId = stream.id
                currentlyPlayingStreamId = stream.id
                loadVideoFromMediaItem(stream.id)
                loadMetadata(stream.id)
                // Reload streams to get the new MediaItem's additional_views
                loadStreamSelections(stream.id)
            }

            is StreamItem.AdditionalView -> {
                currentlyPlayingStreamId = stream.id
                streamSelectionPane?.setCurrentlyPlayingStreamId(currentlyPlayingStreamId)
                // Keep current streams (additional_views belong to the original media item)
                loadVideoFromHash(stream.playableHash, stream.id)
            }
        }
    }

    // region Up Next

    /**
     * Asks the server what plays next while the video is still running, so the card can go up the
     * moment playback stops instead of after a round trip.
     *
     * The boundary message isn't deleted after delivery, so seeking back and watching up to the
     * end again re-arms an offer the viewer previously declined.
     */
    private fun scheduleUpNextPrefetch() {
        val exoPlayer = exoPlayer ?: return
        if (upNextPrefetchMessage != null || !upNextSupported) return
        val duration = exoPlayer.duration
        if (duration == C.TIME_UNSET || exoPlayer.isCurrentMediaItemLive) {
            // Nothing to count down to.
            return
        }
        val prefetchAt = duration - UP_NEXT_PREFETCH_LEAD_MS
        if (prefetchAt <= 0) {
            // Item is shorter than the prefetch window. [offerUpNext] asks for itself instead.
            return
        }
        Log.d("Up next will ask at ${prefetchAt}ms of ${duration}ms (now ${exoPlayer.currentPosition}ms)")
        upNextPrefetchMessage = exoPlayer.createMessage { _, _ -> prefetchUpNext() }
            .setLooper(Looper.getMainLooper())
            .setPosition(prefetchAt)
            .setDeleteAfterDelivery(false)
            .send()
    }

    private fun prefetchUpNext() {
        if (!upNextSupported) return
        val propertyId = navArgs.propertyId ?: return
        // Crossing the boundary again means the viewer watched this ending a second time, which
        // earns a fresh offer even if they declined the first one.
        upNextDeclined = false
        if (upNextDisposable != null || upNextItem != null) return

        upNextDisposable = upNextLoader
            .getNextItem(
                propertyId = propertyId,
                mediaItemId = currentlyPlayingStreamId,
                sectionId = navArgs.sectionId,
                mediaListId = navArgs.mediaListId,
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { next ->
                    Log.d("Up next prefetched ${next.id}")
                    upNextItem = next
                    // Playback can reach the end before the answer does.
                    showUpNextIfReady()
                },
                onComplete = { Log.d("Nothing to play after $currentlyPlayingStreamId") },
            )
            .addTo(disposables)
    }

    /** Playback reached the end: offer the next item, if there is one to offer. */
    private fun offerUpNext() {
        if (upNextDeclined || offeredUpNextItem != null) return
        upNextRequested = true
        // A no-op unless the item was too short for the prefetch window.
        prefetchUpNext()
        showUpNextIfReady()
    }

    /**
     * Puts the card up, once playback has reached the end and the server has answered - whichever
     * order those happen in.
     */
    private fun showUpNextIfReady() {
        if (!upNextRequested || upNextDeclined || offeredUpNextItem != null) return
        val next = upNextItem ?: return
        Log.d("Up next offering ${next.id}")
        offeredUpNextItem = next
        playerView?.hideController()
        playerView?.controllerAutoShow = false
        upNextPane?.show(next)
    }

    private fun declineUpNext() {
        Log.d("Up next declined")
        upNextPane?.hide()
        offeredUpNextItem = null
        upNextDeclined = true
        playerView?.controllerAutoShow = true
        playerView?.showController()
    }

    /**
     * Takes the viewer to the offered item. Autoplay hands back items they aren't entitled to and
     * events that haven't started, so this routes through the same resolver a card tap uses, and
     * hands anything the player can't open itself back to whoever launched it.
     */
    private fun playUpNext(next: MediaEntity) {
        upNextPane?.hide()
        offeredUpNextItem = null
        val propertyId = navArgs.propertyId ?: return
        val permissionContext = PermissionContext(
            propertyId = propertyId,
            pageId = navArgs.pageId,
            sectionId = navArgs.sectionId,
            mediaItemId = next.id,
            mediaListId = navArgs.mediaListId,
        )
        when (val target = next.onClickTarget(permissionContext)) {
            is VideoPlayerArgs -> {
                Log.d("Up next playing ${next.id}")
                switchToStream(StreamItem.MediaItem.from(next))
            }

            is PurchasePromptNavArgs -> exitTo(VideoPlayerExit.Purchase(target))
            is UpcomingVideoNavArgs -> exitTo(VideoPlayerExit.Upcoming(target))

            else -> {
                // Nothing we can open from here. Leave the viewer on the finished video rather
                // than closing the player out from under them.
                Log.w("Up next item ${next.id} has nowhere to go: $target")
                declineUpNext()
            }
        }
    }

    /** Closes the player and asks whoever launched it to open [exit] in its place. */
    private fun exitTo(exit: VideoPlayerExit) {
        setResult(
            RESULT_OK,
            Intent().putExtra(
                VIDEO_PLAYER_EXIT_EXTRA,
                Json.encodeToString(VideoPlayerExit.serializer(), exit)
            )
        )
        finish()
    }

    private fun resetUpNext() {
        upNextPane?.hide()
        upNextDisposable.safeDispose()
        upNextDisposable = null
        // Otherwise it's still pending against the item we just left.
        upNextPrefetchMessage?.cancel()
        upNextPrefetchMessage = null
        upNextItem = null
        offeredUpNextItem = null
        upNextRequested = false
        upNextDeclined = false
    }

    /**
     * Up Next only makes sense for media items played inside a Property. The deeplink demo path
     * fakes its media item, and additional views aren't media items at all.
     */
    private val upNextSupported: Boolean
        get() = navArgs.propertyId != null &&
                navArgs.deeplinkhack_contract == null &&
                // Additional views are fabric objects, not media items the server knows a run for.
                !currentlyPlayingStreamId.startsWith(ADDITIONAL_VIEW_ID_PREFIX)

    // endregion

    // ExoPlayer has a hard time when "bottom bar" is higher than 50% of the screen,
    // so we need to manually position the thumbnail view above the time bar.
    private fun positionThumbnailAboveTimeBar() {
        val timeBarView = timeBar ?: return
        val thumbnailView = scrubThumbnailView ?: return

        // Get the TimeBar's position relative to its parent (the root FrameLayout)
        val timeBarLocation = IntArray(2)
        timeBarView.getLocationInWindow(timeBarLocation)

        val parentLocation = IntArray(2)
        (thumbnailView.parent as? View)?.getLocationInWindow(parentLocation)

        // Position the thumbnail view so its bottom is at the top of the TimeBar
        val timeBarTopRelativeToParent = timeBarLocation[1] - parentLocation[1]
        val padding = 18f * resources.displayMetrics.density
        thumbnailView.y = timeBarTopRelativeToParent - thumbnailView.thumbnailHeight - padding
        thumbnailView.x = timeBarLocation[0].toFloat()
        thumbnailView.layoutParams.width = timeBarView.width
    }
}

private val LIVE_EDGE_PROXIMITY_THRESHOLD = 20.seconds.inWholeMilliseconds

/** How long before the end we ask the server what plays next. */
private val UP_NEXT_PREFETCH_LEAD_MS = 30.seconds.inWholeMilliseconds
