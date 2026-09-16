package app.eluvio.wallet.screens.property.search

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PropertySearchNavArgs(
    val propertyId: String,
    /**
     * Filter values to open with already selected, set by "search_page_link" section items.
     * Null means no preselection - the property's default filter applies.
     */
    val primaryFilter: String? = null,
    val secondaryFilter: String? = null,
) : NavKey
