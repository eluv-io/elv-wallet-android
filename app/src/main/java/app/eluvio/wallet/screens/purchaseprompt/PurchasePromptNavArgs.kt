package app.eluvio.wallet.screens.purchaseprompt

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.data.permissions.PermissionContext
import kotlinx.serialization.Serializable

@Serializable
data class PurchasePromptNavArgs(
    val permissionContext: PermissionContext,
    /**
     *  When provided, will just prompt to view the specified pageId, without including a "context" param for the web client
     */
    val pageOverride: String? = null,
) : NavKey
