package app.eluvio.wallet.data.entities.v2.permissions

/**
 * Permission settings for an object.
 */
interface PermissionSettings {
    // Permission items required to access this object.
    val permissionItemIds: List<String>

    // Content permissions trickles down to children.
    val behavior: String?
    val alternatePageId: String?

    // We just pass it as-is when launching purchase options
    val secondaryMarketPurchaseOption: String?

    // Only applies to resolve permissions
    val authorized: Boolean?
}

/**
 * A straightforward implementation of [PermissionSettings] for fields that are explicitly not
 * meant to be persisted in Realm.
 */
data class VolatilePermissionSettings(
    // Permission items required to access this object.
    override val permissionItemIds: List<String>,

    // Content permissions trickles down to children.
    override val behavior: String?,
    override val alternatePageId: String?,
    // We just pass it as-is when launching purchase options
    // TODO: add prop/page equivalents
    override val secondaryMarketPurchaseOption: String?,

    // Only applies to resolve permissions
    override val authorized: Boolean?
) : PermissionSettings