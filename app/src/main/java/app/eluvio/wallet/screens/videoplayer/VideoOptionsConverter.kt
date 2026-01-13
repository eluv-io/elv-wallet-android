package app.eluvio.wallet.screens.videoplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import app.eluvio.wallet.di.TokenAwareHttpClient
import app.eluvio.wallet.network.dto.PlayoutConfigDto
import app.eluvio.wallet.network.dto.VideoOptionsDto
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.media.FormatCapabilityChecker
import okhttp3.OkHttpClient

/**
 * Contains the media source for playback along with optional thumbnail info.
 */
data class VideoPlayoutInfo(
    val mediaSource: MediaSource,
    val thumbnailsWebVttUrl: String?
)

/**
 * Converts video options to a [VideoPlayoutInfo] containing both the media source
 * and thumbnail WebVTT URL for scrubbing previews.
 */
@OptIn(UnstableApi::class)
fun VideoOptionsDto.toVideoPlayoutInfo(
    baseUrl: String,
    context: Context,
    @TokenAwareHttpClient httpClient: OkHttpClient
): VideoPlayoutInfo? {
    val config = selectBestFormat() ?: return null

    val drm = config.properties.drm ?: DRM_CLEAR
    val uri = "${baseUrl.removeSuffix("/")}/${config.uri}"
    val thumbnailsUrl = config.properties.thumbnailsWebvttUri?.let { thumbUri ->
        // thumbnail uri starts with the offering name (e.g. "dash-clear"), and exists in the same
        // path as the video uri.
        val (offering, restOfPath) = thumbUri.split("/", limit = 2)
        uri.replaceAfter("/${offering}/", restOfPath, "")
            .takeIf { it.isNotEmpty() }
    }

    val dataSourceFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(httpClient))
    val drmBuilder = MediaItem.DrmConfiguration.Builder(C.UUID_NIL)
    when (drm) {
        DRM_WIDEVINE -> {
            drmBuilder.setScheme(C.WIDEVINE_UUID)
                .setLicenseUri(config.properties.license_servers?.firstOrNull())
                .setMultiSession(true)
        }

        DRM_CLEAR -> {
            drmBuilder.setScheme(C.CLEARKEY_UUID)
                .setMultiSession(true)
        }

        else -> throw RuntimeException("Unsupported DRM type $drm")
    }
    val protocol = config.properties.protocol
    val mediaSourceFactory = when (protocol) {
        PROTOCOL_DASH -> DashMediaSource.Factory(dataSourceFactory)
        PROTOCOL_HLS -> HlsMediaSource.Factory(dataSourceFactory)
        else -> throw RuntimeException("Unsupported protocol $protocol")
    }
    Log.i("Loading ${protocol}-${drm}, thumbnails: ${thumbnailsUrl != null}")

    val mediaItem = makeMediaItem(uri, drmBuilder.build())

    val mediaSource = mediaSourceFactory
        .setDrmSessionManagerProvider(DefaultDrmSessionManagerProvider().apply {
            setDrmHttpDataSourceFactory(dataSourceFactory)
        })
        .createMediaSource(mediaItem)

    return VideoPlayoutInfo(
        mediaSource = mediaSource,
        thumbnailsWebVttUrl = thumbnailsUrl
    )
}

/**
 * Selects the best playout format based on device codec capabilities.
 * Priority: Clear > Widevine (DRM), with DASH/HLS preference based on device support.
 */
private fun VideoOptionsDto.selectBestFormat(): PlayoutConfigDto? {
    val preference = FormatCapabilityChecker.getPreferredFormat()
    val capabilities = FormatCapabilityChecker.getCapabilities()

    Log.d("Format selection - preference: $preference, reason: ${capabilities.reason}")

    return when (preference) {
        FormatCapabilityChecker.PreferredFormat.DASH -> {
            dash_clear ?: dash_widevine ?: hls_clear ?: hls_widevine
        }

        FormatCapabilityChecker.PreferredFormat.HLS -> {
            hls_clear ?: hls_widevine ?: dash_clear ?: dash_widevine
        }

        FormatCapabilityChecker.PreferredFormat.NO_PREFERENCE -> {
            dash_clear ?: hls_clear ?: dash_widevine ?: hls_widevine
        }
    }
}

private fun makeMediaItem(
    uri: String,
    drmConfiguration: MediaItem.DrmConfiguration
): MediaItem {
    return MediaItem.Builder()
        .setUri(uri)
        .setDrmConfiguration(drmConfiguration)
        .build()
}

private const val PROTOCOL_DASH = "dash"
private const val PROTOCOL_HLS = "hls"

private const val DRM_CLEAR = "clear"
private const val DRM_WIDEVINE = "widevine"
