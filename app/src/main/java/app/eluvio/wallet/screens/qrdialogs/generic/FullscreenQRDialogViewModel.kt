package app.eluvio.wallet.screens.qrdialogs.generic

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.screens.common.generateQrCode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class FullscreenQRDialogViewModel @Inject constructor(
    private val urlShortener: UrlShortener,
    private val navArgs: FullscreenQRDialogNavArgs,
) : BaseViewModel<FullscreenQRDialogViewModel.State>(
    State(
        title = navArgs.title,
        subtitle = navArgs.subtitleOverride
    )
) {

    @Immutable
    data class State(
        val title: String,
        val subtitle: String? = null,
        val qrImage: Bitmap? = null,
    )

    override fun onResume() {
        super.onResume()

        val displayUrl = if (navArgs.shortenUrl) {
            urlShortener.shorten(navArgs.url)
                .onErrorReturnItem(navArgs.url)
        } else {
            Single.just(navArgs.url)
        }

        displayUrl
            .flatMap { url ->
                generateQrCode(url)
                    .map { qr -> url to qr }
            }
            .subscribeBy { (url, qr) ->
                updateState {
                    copy(
                        subtitle = navArgs.subtitleOverride ?: url.takeIf { it.length < 30 },
                        qrImage = qr
                    )
                }
            }
            .addTo(disposables)
    }
}
