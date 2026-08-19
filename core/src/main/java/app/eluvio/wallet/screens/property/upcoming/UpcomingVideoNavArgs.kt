package app.eluvio.wallet.screens.property.upcoming

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class UpcomingVideoNavArgs(
    val propertyId: String,
    val mediaItemId: String,
) : NavKey
