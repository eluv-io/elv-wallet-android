package app.eluvio.wallet.screens.purchaseprompt

import app.eluvio.wallet.data.permissions.PermissionContext

data class PurchasePromptNavArgs(
    val permissionContext: PermissionContext,
    /**
     *  When provided, will just prompt to view the specified pageId, without including a "context" param for the web client
     */
    val pageOverride: String? = null
)
