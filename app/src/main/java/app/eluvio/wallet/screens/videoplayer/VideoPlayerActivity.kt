package app.eluvio.wallet.screens.videoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import app.eluvio.wallet.R
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.videoplayer.ui.ScrubThumbnailView
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
import com.ramcosta.composedestinations.annotation.ActivityDestination
import com.ramcosta.composedestinations.generated.destinations.VideoPlayerActivityDestination
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.media3.ui.R as media3R

@ActivityDestination<MainGraph>(navArgs = VideoPlayerArgs::class)
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

    private var disposables = CompositeDisposable()

    private var playerView: PlayerView? = null
    private var exoPlayer: ExoPlayer? = null

    private var playPauseButton: View? = null
    private var timeBar: DefaultTimeBar? = null
    private var scrubThumbnailView: ScrubThumbnailView? = null

    private var liveIndicator: View? = null
    private var infoButton: View? = null
    private var infoPane: VideoInfoPane? = null

    // List of buttons that aren't handled by exoplayer, and we need to handle manually.
    private val customControllerButtons: List<View>
        get() = listOfNotNull(liveIndicator, infoButton)

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

    private val navArgs by lazy { VideoPlayerActivityDestination.argsFrom(intent) }
    private val mediaItemId by lazy { fakeMediaItemId ?: navArgs.mediaItemId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        playPauseButton = findViewById(media3R.id.exo_play_pause)
        timeBar = findViewById(media3R.id.exo_progress)
        timeBar?.setKeyTimeIncrement(5000)

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
                    loadVideo(mediaItemId)
                    loadMetadata(mediaItemId)
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

    private fun loadVideo(mediaItemId: String) {
        // this is all we really need if it wasn't for all the fake stuff
        Singles.zip(
            videoOptionsFetcher.fetchVideoOptions(mediaItemId, navArgs.propertyId),
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
                    exoPlayer?.seekTo(playbackStore.getPlaybackPosition(mediaItemId))
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
                    mediaItemId,
                    currentPosition,
                    exoPlayer.contentDuration
                )
            } else {
                playbackStore.setPlaybackPosition(mediaItemId, 0, exoPlayer.contentDuration)
            }
        }
        playerView?.onPause()
        // Stop player to release resources, instead of just pausing.
        playerView?.player?.stop()
    }

    override fun onDestroy() {
        Log.d("onDestroy")
        playerView = null
        exoPlayer?.release()
        exoPlayer = null
        disposables.safeDispose()
        disposables = CompositeDisposable()
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (infoPane?.isVisible == true) {
            Log.d("Forwarding key event to info pane")
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                infoPane?.visibility = View.GONE
                playerView?.showController()
                return true
            }
            return infoPane?.dispatchKeyEvent(event) == true
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
