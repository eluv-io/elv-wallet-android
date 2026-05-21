package app.eluvio.wallet.screens.deeplink

data class NftClaimNavArgs(
    val marketplace: String,
    val sku: String,
    val signedEntitlementMessage: String?,
    val backLink: String?
)
