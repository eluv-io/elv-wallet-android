package app.eluvio.wallet.data.entities.v2.permissions

val PermissionSettings.isHidden: Boolean
    get() = (authorized == false && behaviorEnum == PermissionBehavior.HIDE) ||
            (authorized == true && behaviorEnum == PermissionBehavior.ONLY_SHOW_IF_UNAUTHORIZED) ||
            isInvalidPurchaseOptionsState

val PermissionSettings.isDisabled: Boolean
    get() = authorized == false && behaviorEnum == PermissionBehavior.DISABLE

val PermissionSettings.showPurchaseOptions: Boolean
    get() = authorized == false && behaviorEnum == PermissionBehavior.SHOW_PURCHASE

val PermissionSettings.showAlternatePage: Boolean
    get() = authorized == false && behaviorEnum == PermissionBehavior.SHOW_ALTERNATE_PAGE && alternatePageId != null

val PermissionSettings.behaviorEnum: PermissionBehavior?
    get() = behavior?.let { PermissionBehavior.fromValue(it) }

private val PermissionSettings.isInvalidPurchaseOptionsState: Boolean
    // If the only permissions item is "", then this is a private item that can't be accessed by anyone.
    get() = showPurchaseOptions && permissionItemIds.all { it == "" }
