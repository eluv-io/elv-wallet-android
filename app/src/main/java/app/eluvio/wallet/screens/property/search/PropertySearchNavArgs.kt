package app.eluvio.wallet.screens.property.search

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PropertySearchNavArgs(
    val propertyId: String,
    val primaryFilter: String? = null
) : NavKey
