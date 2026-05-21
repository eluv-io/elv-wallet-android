package app.eluvio.wallet.screens.nftdetail.legacy

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class LegacyNftDetailArgs(
    val contractAddress: String,
    val tokenId: String,
    val marketplaceId: String? = null,
    val backLink: String? = null,
) : NavKey
