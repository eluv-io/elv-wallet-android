package app.eluvio.wallet.screens.qrdialogs.generic

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.screens.common.generateQrCode
import com.stavfx.nav3hiltvm.annotations.HiltNavKeyViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

@HiltNavKeyViewModel
open class FullscreenQRDialogViewModel(
    @NavArg private val navArgs: FullscreenQRDialogNavArgs,
    private val urlShortener: UrlShortener,
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
