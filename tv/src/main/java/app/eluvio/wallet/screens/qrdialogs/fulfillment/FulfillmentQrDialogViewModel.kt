package app.eluvio.wallet.screens.qrdialogs.fulfillment

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.stores.FulfillmentStore
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.mapNotNull
import com.stavfx.nav3hiltvm.annotations.HiltNavArgViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

@HiltNavArgViewModel
open class FulfillmentQrDialogViewModel(
    @NavArg private val navArgs: FulfillmentQrDialogNavArgs,
    private val fulfillmentStore: FulfillmentStore,
) : BaseViewModel<FulfillmentQrDialogViewModel.State>(State()) {

    @Immutable
    data class State(
        val loading: Boolean = true,
        val code: String = "",
        val qrBitmap: Bitmap? = null
    )

    override fun onResume() {
        super.onResume()

        fulfillmentStore.observeFulfillmentData(navArgs.transactionHash)
            .mapNotNull {
                val url = it.url ?: return@mapNotNull null
                val code = it.code ?: return@mapNotNull null
                url to code
            }
            .switchMapSingle { (url, code) ->
                generateQrCode(url).map { State(loading = false, code, qrBitmap = it) }
            }
            .subscribeBy(
                onNext = { updateState { it } },
                onError = {
                    Log.e(
                        "Error loading fulfillment for transaction ${navArgs.transactionHash}",
                        it
                    )
                }
            )
            .addTo(disposables)
    }
}
