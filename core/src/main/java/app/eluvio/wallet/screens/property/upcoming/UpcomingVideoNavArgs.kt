package app.eluvio.wallet.screens.property.upcoming

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class UpcomingVideoNavArgs(
    val propertyId: String,
    val mediaItemId: String,
    // The PageID of the source page that navigated to this page.
    // Used to display the correct background image.
    val sourcePageId: String? = null,
) : NavKey
