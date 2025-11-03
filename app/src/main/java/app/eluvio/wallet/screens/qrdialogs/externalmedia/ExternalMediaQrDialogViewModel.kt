package app.eluvio.wallet.screens.qrdialogs.externalmedia

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.util.logging.Log
import com.ramcosta.composedestinations.generated.destinations.ExternalMediaQrDialogDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ExternalMediaQrDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentStore: ContentStore,
    private val tokenStore: TokenStore,
    private val apiProvider: ApiProvider,
    private val urlShortener: UrlShortener,
) : BaseViewModel<ExternalMediaQrDialogViewModel.State>(State(), savedStateHandle) {

    @Immutable
    @Parcelize
    data class State(
        val qrCode: Bitmap? = null,
        val url: String? = null,
        val error: Boolean = false
    ) : Parcelable

    private val mediaId = ExternalMediaQrDialogDestination.argsFrom(savedStateHandle).mediaItemId
    override fun onResume() {
        super.onResume()
        apiProvider.getFabricEndpoint()
            .flatMapPublisher { endpoint ->
                contentStore.observeMediaItem(mediaId)
                    .map { media -> getAuthorizedUrl(endpoint, media) }
            }
            .distinctUntilChanged()
            .switchMapSingle { fullUrl ->
                // Try to shorten
                urlShortener.shorten(fullUrl)
                    // Fall back to full URL if shortening fails
                    .onErrorReturnItem(fullUrl)
            }
            .switchMapSingle { url ->
                // Generate QR for whatever URL we have
                generateQrCode(url)
                    .map { it to url }
            }
            .subscribeBy(
                onNext = { (qrCode, url) ->
                    updateState {
                        copy(
                            qrCode = qrCode,
                            // Only display URLs if they were shortened successfully
                            url = url.takeIf { it.length < 30 },
                            error = false
                        )
                    }
                },
                onError = {
                    updateState { copy(error = true) }
                    Log.e("Error loading QR for media item:$mediaId", it)
                }
            )
            .addTo(disposables)
    }

    private fun getAuthorizedUrl(baseUrl: String, media: MediaEntity): String {
        val url = Uri.parse(
            buildString {
                append(
                    media.mediaFile.takeIf { it.isNotEmpty() }
                        ?: media.mediaLinks.values.first()
                )
                if (!startsWith("http")) {
                    insert(0, baseUrl)
                }
            }
        )
        val token = tokenStore.fabricToken.get()
        return if (token != null && url.getQueryParameter("authorization") == null) {
            url.buildUpon()
                .appendQueryParameter("authorization", token)
                .build()
                .toString()
        } else {
            url.toString()
        }
    }
}
