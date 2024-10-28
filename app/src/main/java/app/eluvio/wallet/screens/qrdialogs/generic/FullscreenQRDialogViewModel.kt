package app.eluvio.wallet.screens.qrdialogs.generic

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.data.stores.encodedAuthParam
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
    private val environmentStore: EnvironmentStore,
    private val tokenStore: TokenStore,
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

        val fullUrl = (if (navArgs.urlOrWalletPath.startsWith("/")) {
            environmentStore.observeSelectedEnvironment()
                .firstOrError()
                .map { it.walletUrl + navArgs.urlOrWalletPath }
        } else {
            Single.just(navArgs.urlOrWalletPath)
        })
            .map {
                if (navArgs.appendAuthToken) {
                    val url = Uri.parse(it)
                    if (url.getQueryParameter("authorization") == null) {
                        val token = tokenStore.encodedAuthParam()
                        url.buildUpon()
                            .appendQueryParameter("authorization", token)
                            .build()
                            .toString()
                    } else {
                        url.toString()
                    }
                } else {
                    it
                }
            }

        val displayUrl = if (navArgs.shortenUrl) {
            fullUrl.flatMap {
                urlShortener.shorten(it)
                    .onErrorReturnItem(it)
            }
        } else {
            fullUrl
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
