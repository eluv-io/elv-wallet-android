package app.eluvio.wallet.screens.videoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import app.eluvio.wallet.R
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.destinations.VideoPlayerActivityDestination
import app.eluvio.wallet.util.crypto.Base58
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import app.eluvio.wallet.util.rx.safeDispose
import com.ramcosta.composedestinations.annotation.ActivityDestination
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@MainGraph
@ActivityDestination(navArgsDelegate = VideoPlayerArgs::class)
@AndroidEntryPoint
@UnstableApi
class VideoPlayerActivity : FragmentActivity(), Player.Listener {
    @Inject
    lateinit var videoOptionsFetcher: VideoOptionsFetcher

    @Inject
    lateinit var playbackStore: PlaybackStore

    private var disposable: Disposable? = null

    private var playerView: PlayerView? = null
    private var exoPlayer: ExoPlayer? = null
    private var liveIndicator: View? = null

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // No need to propagate event.
            // This callback should only be enabled when the controller is visible.
            playerView?.hideController()
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

        liveIndicator = findViewById(R.id.live_indicator)
        liveIndicator?.setOnClickListener {
            exoPlayer?.seekToDefaultPosition()
            exoPlayer?.playWhenReady = true
        }

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            .build()
            .apply {
                addListener(this@VideoPlayerActivity)
                playWhenReady = true
            }
        playerView = findViewById<PlayerView>(R.id.video_player_view)?.apply {
            setShowSubtitleButton(true)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            controllerHideOnTouch = false
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                backPressedCallback.isEnabled = visibility == View.VISIBLE
                Log.v("Controller visibility changed: $visibility. Handling back press: ${backPressedCallback.isEnabled}")
            })
            player = exoPlayer
            showController()

            // Manually show spinner until exoplayer figures itself out
            //noinspection MissingInflatedId
            findViewById<View>(androidx.media3.ui.R.id.exo_buffering).visibility = View.VISIBLE
        }

        //noinspection MissingInflatedId
        findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
            ?.setKeyTimeIncrement(5000)

        disposable = Maybe.fromCallable { navArgs.deeplinkhack_contract }
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
            .defaultIfEmpty("done with fake stuff, time to load video")
            .flatMap {
                // this is all we really need if it wasn't for all the fake stuff
                videoOptionsFetcher.fetchVideoOptions(mediaItemId, navArgs.propertyId)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    exoPlayer?.setMediaSource(it.toMediaSource())
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
    }

    override fun onEvents(player: Player, events: Player.Events) {
        // re-calc live state on every event
        updateLiveTagState()
    }

    private fun updateLiveTagState() {
        val exoPlayer = exoPlayer ?: return
        val isLive = exoPlayer.isCurrentMediaItemLive
        liveIndicator?.isVisible = isLive
        if (isLive) {
            /**
             * [ExoPlayer.getCurrentLiveOffset] didn't work as expected, so just using proximity to end of seekbar instead.
             */
            val isCloseToLiveEdge =
                exoPlayer.contentDuration - exoPlayer.contentPosition < 20.seconds.inWholeMilliseconds
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
        playerView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.currentPosition?.let { currentPosition ->
            if (shouldStorePlaybackPosition(currentPosition)) {
                playbackStore.setPlaybackPosition(mediaItemId, currentPosition)
            } else {
                playbackStore.setPlaybackPosition(mediaItemId, 0)
            }
        }
        playerView?.onPause()
        playerView?.player?.pause()
    }

    override fun onDestroy() {
        playerView = null
        exoPlayer?.release()
        exoPlayer = null
        disposable.safeDispose()
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
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
}
