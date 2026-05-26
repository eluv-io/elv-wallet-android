package app.eluvio.wallet.screens.nftdetail

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NftDetailNavArgs(
    val contractAddress: String,
    val tokenId: String,
) : NavKey
