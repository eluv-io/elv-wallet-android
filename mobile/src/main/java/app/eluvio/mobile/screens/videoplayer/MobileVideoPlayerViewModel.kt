package app.eluvio.mobile.screens.videoplayer

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import app.eluvio.wallet.data.VideoOptionsFetcher
import app.eluvio.wallet.data.stores.PlaybackStore
import app.eluvio.wallet.screens.videoplayer.VideoPlayerArgs
import app.eluvio.wallet.util.logging.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.inject.Inject

/**
 * Owns the [ExoPlayer] across config-change recreation of [MobileVideoPlayerFragment].
 * The fragment attaches/detaches the player to its [androidx.media3.ui.PlayerView] via the
 * view lifecycle; this VM keeps the player buffering and the playout request in-flight so
 * rotation doesn't re-fetch or re-buffer the stream.
 */
@HiltViewModel
@UnstableApi
class MobileVideoPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    videoOptionsFetcher: VideoOptionsFetcher,
    private val playbackStore: PlaybackStore,
) : ViewModel() {

    private val args: VideoPlayerArgs = savedStateHandle.toRoute()
    private val disposables = CompositeDisposable()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
        .build()

    /** Emits if the playout fetch fails, so the fragment can surface an error + pop back. */
    private val _loadErrors = PublishSubject.create<Throwable>()
    val loadErrors: Observable<Throwable> = _loadErrors.hide()

    /**
     * Pause when the whole app backgrounds. Process-level (not fragment/view) lifecycle so a
     * rotation — which stops the activity briefly — doesn't pause the stream.
     */
    private val backgroundPauser = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            exoPlayer.pause()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundPauser)
        videoOptionsFetcher.fetchVideoOptions(args.mediaItemId, args.propertyId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { playoutInfo ->
                    exoPlayer.setMediaSource(playoutInfo.mediaSource)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                    exoPlayer.seekTo(playbackStore.getPlaybackPosition(args.mediaItemId))
                },
                onError = {
                    Log.e("Error loading video for ${args.mediaItemId}", it)
                    _loadErrors.onNext(it)
                },
            )
            .addTo(disposables)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(backgroundPauser)
        playbackStore.setPlaybackPosition(
            args.mediaItemId,
            exoPlayer.currentPosition,
            exoPlayer.contentDuration,
        )
        exoPlayer.release()
        disposables.clear()
        super.onCleared()
    }
}
