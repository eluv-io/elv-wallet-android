package app.eluvio.wallet.screens.deeplink

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class NftClaimNavArgs(
    val marketplace: String,
    val sku: String,
    val signedEntitlementMessage: String? = null,
    val backLink: String? = null,
) : NavKey
