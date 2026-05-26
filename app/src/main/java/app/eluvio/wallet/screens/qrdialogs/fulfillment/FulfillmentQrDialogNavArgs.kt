package app.eluvio.wallet.screens.qrdialogs.fulfillment

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class FulfillmentQrDialogNavArgs(val transactionHash: String) : NavKey
