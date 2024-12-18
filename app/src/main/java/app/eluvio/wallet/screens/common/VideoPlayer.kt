package app.eluvio.wallet.screens.common

import android.annotation.SuppressLint
import android.os.HandlerThread
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import app.eluvio.wallet.util.logging.Log

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    mediaSource: MediaSource,
    modifier: Modifier = Modifier,
    // Fallback content to show when the player encounters an error
    fallback: @Composable (() -> Unit)? = null
) {
    var showFallback by rememberSaveable { mutableStateOf(false) }
    if (showFallback && fallback != null) {
        fallback()
        return
    }

    val context = LocalContext.current

    // Do not recreate the player everytime this Composable commits
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setPlaybackLooper(videoPlayerLooper)
            .build()
            .apply {
                setMediaSource(mediaSource)
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("PlayerError", error)
                        showFallback = true
                    }
                })
                prepare()
            }
    }
    var lifecycle by remember { mutableStateOf(Lifecycle.Event.ON_CREATE) }
    // Gateway to traditional Android Views
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                player = exoPlayer
            }
        },
        update = {
            when (lifecycle) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("Compose VideoPlayer onPause")
                    it.onPause()
                    // .pause doesn't release resources (decoders) so we call .stop
                    exoPlayer.stop()
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.d("Compose VideoPlayer onResume")
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                    it.onResume()
                }

                else -> {}
            }
        }
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
}

/**
 * Share a single Looper for all ExoPlayer instances.
 */
private val videoPlayerLooper: Looper = HandlerThread("videoPlayerLooper")
    .apply { start() }
    .looper
