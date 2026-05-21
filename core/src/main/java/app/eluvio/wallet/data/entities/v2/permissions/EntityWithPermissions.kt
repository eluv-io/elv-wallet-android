package app.eluvio.wallet.data.entities.v2.permissions

interface EntityWithPermissions {
    // Permissions directly set on this object, as they came from the server.
    // Does not consider parent hierarchy.
    val rawPermissions: PermissionSettingsEntity?

    // "Actual" permissions, after considering parent hierarchy. Should not be persisted.
    var resolvedPermissions: VolatilePermissionSettings?

    // Direct children of this object that have permissions.
    val permissionChildren: List<EntityWithPermissions>

    val isHidden: Boolean get() = bestPermissions?.isHidden == true
    val isDisabled: Boolean get() = bestPermissions?.isDisabled == true
    val showPurchaseOptions: Boolean get() = bestPermissions?.showPurchaseOptions == true
    val showAlternatePage: Boolean get() = bestPermissions?.showAlternatePage == true
}

private val EntityWithPermissions.bestPermissions: PermissionSettings?
    get() = resolvedPermissions ?: rawPermissions
