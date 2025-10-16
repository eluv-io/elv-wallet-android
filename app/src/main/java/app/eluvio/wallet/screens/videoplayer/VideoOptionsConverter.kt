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
import app.eluvio.wallet.network.dto.VideoOptionsDto
import app.eluvio.wallet.util.logging.Log
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
fun VideoOptionsDto.toMediaSource(
    baseUrl: String,
    context: Context,
    @TokenAwareHttpClient httpClient: OkHttpClient
): MediaSource? {
    val config = dash_clear
        ?: hls_clear
        ?: dash_widevine
        ?: hls_widevine
        // Couldn't find a supported playback format
        ?: return null

    val drm = config.properties.drm ?: DRM_CLEAR
    val uri = "${baseUrl.removeSuffix("/")}/${config.uri}"

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
    Log.i("Loading ${protocol}-${drm}")

    val mediaItem = makeMediaItem(uri, drmBuilder.build())

    return mediaSourceFactory
        .setDrmSessionManagerProvider(DefaultDrmSessionManagerProvider().apply {
            // DrmSessionManager should use the same http stack so Token refreshing is handled
            // correctly
            setDrmHttpDataSourceFactory(dataSourceFactory)
        })
        .createMediaSource(mediaItem)
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
