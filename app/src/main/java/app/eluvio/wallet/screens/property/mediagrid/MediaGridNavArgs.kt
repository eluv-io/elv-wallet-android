package app.eluvio.wallet.screens.property.mediagrid

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.data.GridContentOverride
import app.eluvio.wallet.data.permissions.PermissionContext
import kotlinx.serialization.Serializable

@Serializable
data class MediaGridNavArgs(
    val permissionContext: PermissionContext,
    val gridContentOverride: GridContentOverride? = null,
) : NavKey
