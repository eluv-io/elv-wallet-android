package app.eluvio.wallet.screens.deeplink

import kotlinx.serialization.Serializable

@Serializable
data class NftClaimNavArgs(
    val marketplace: String,
    val sku: String,
    val signedEntitlementMessage: String? = null,
    val backLink: String? = null,
)
