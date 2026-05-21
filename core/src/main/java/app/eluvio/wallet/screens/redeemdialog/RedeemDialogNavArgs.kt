package app.eluvio.wallet.screens.redeemdialog

import kotlinx.serialization.Serializable

@Serializable
data class RedeemDialogNavArgs(
    val contractAddress: String,
    val tokenId: String,
    val offerId: String
)
