package app.eluvio.wallet.screens.redeemdialog

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RedeemDialogNavArgs(
    val contractAddress: String,
    val tokenId: String,
    val offerId: String
) : NavKey
