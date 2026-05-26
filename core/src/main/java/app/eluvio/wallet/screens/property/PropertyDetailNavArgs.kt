package app.eluvio.wallet.screens.property

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.data.PropertyLink
import kotlinx.serialization.Serializable

@Serializable
data class PropertyDetailNavArgs(
    val propertyId: String,
    /** Only required to navigate to a specific page. Usually due to showAltPage permission behavior */
    val pageId: String? = null,
    val propertyLinks: List<PropertyLink> = emptyList(),
) : NavKey
