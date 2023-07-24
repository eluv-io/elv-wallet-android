package app.eluvio.wallet.screens.qrdialogs.fulfillment

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.stores.FulfillmentStore
import app.eluvio.wallet.screens.common.generateQrCode
import app.eluvio.wallet.screens.destinations.FulfillmentQrDialogDestination
import app.eluvio.wallet.util.rx.mapNotNull
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@HiltViewModel
class FulfillmentQrDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fulfillmentStore: FulfillmentStore,
) : BaseViewModel<FulfillmentQrDialogViewModel.State>(State()) {
    data class State(val code: String = "", val qrBitmap: Bitmap? = null)

    private val transactionHash =
        FulfillmentQrDialogDestination.argsFrom(savedStateHandle).transactionHash

    override fun onResume() {
        super.onResume()

        fulfillmentStore.observeFulfillmentData(transactionHash)
            .mapNotNull {
                val url = it.url ?: return@mapNotNull null
                val code = it.code ?: return@mapNotNull null
                url to code
            }
            .switchMapSingle { (url, code) ->
                generateQrCode(url).map { State(code, it) }
            }
            .subscribeBy(
                onNext = { updateState { it } },
                onError = {}
            )
            .addTo(disposables)
    }
}
