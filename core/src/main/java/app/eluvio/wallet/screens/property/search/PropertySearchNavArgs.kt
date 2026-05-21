package app.eluvio.wallet.screens.property.search

import kotlinx.serialization.Serializable

@Serializable
data class PropertySearchNavArgs(
    val propertyId: String,
    val primaryFilter: String? = null
)
