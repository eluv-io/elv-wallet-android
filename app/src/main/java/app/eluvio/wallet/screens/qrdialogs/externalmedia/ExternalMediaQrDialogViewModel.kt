package app.eluvio.wallet.screens.qrdialogs.externalmedia

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.screens.destinations.ExternalMediaQrDialogDestination
import app.eluvio.wallet.util.logging.Log
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
    private val apiProvider: ApiProvider
) : BaseViewModel<ExternalMediaQrDialogViewModel.State>(State(), savedStateHandle) {
    @Parcelize
    data class State(val qrCode: Bitmap? = null, val error: Boolean = false) : Parcelable

    private val mediaId = ExternalMediaQrDialogDestination.argsFrom(savedStateHandle).mediaItemId
    override fun onResume() {
        super.onResume()
        apiProvider.getFabricEndpoint()
            .flatMapPublisher { endpoint ->
                contentStore.observeMediaItem(mediaId)
                    .switchMapSingle { media ->
                        generateQrCode(getAuthorizedUrl(endpoint, media))
                    }
            }
            .subscribeBy(
                onNext = {
                    updateState { copy(qrCode = it, error = false) }
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
