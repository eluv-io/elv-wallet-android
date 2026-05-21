package app.eluvio.wallet.network.converters.v2.permissions

import app.eluvio.wallet.data.entities.v2.permissions.PermissionBehavior
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.network.dto.v2.permissions.PermissionsDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty

// PermissionsDto usually just has Content Permission settings, but sometimes it also hold specific
// permissions for pages and properties.

fun PermissionsDto.toContentPermissionsEntity(): PermissionSettingsEntity {
    val dto = this
    return PermissionSettingsEntity().apply {
        // Content permissions
        permissionItemIds = dto.permission_item_ids.toRealmListOrEmpty()
        behavior = dto.behavior?.ifEmpty { null }
        alternatePageId = dto.alternate_page_id?.ifEmpty { null }
        secondaryMarketPurchaseOption =
            dto.secondary_market_purchase_option?.ifEmpty { null }
    }
}

fun PermissionsDto.toPagePermissionsEntity(): PermissionSettingsEntity {
    val dto = this
    return PermissionSettingsEntity().apply {
        // Page permissions
        permissionItemIds = dto.page_permissions.toRealmListOrEmpty()
        behavior = dto.page_permissions_behavior?.ifEmpty { null }
        alternatePageId = dto.page_permissions_alternate_page_id?.ifEmpty { null }
        secondaryMarketPurchaseOption =
            dto.page_permissions_secondary_market_purchase_option?.ifEmpty { null }
    }
}

fun PermissionsDto.toPropertyPermissionsEntity(): PermissionSettingsEntity {
    val dto = this
    return PermissionSettingsEntity().apply {
        // Property permissions
        permissionItemIds = dto.property_permissions.toRealmListOrEmpty()
        behavior = dto.property_permissions_behavior?.ifEmpty { null }
        alternatePageId = dto.property_permissions_alternate_page_id?.ifEmpty { null }
        secondaryMarketPurchaseOption =
            dto.property_permissions_secondary_market_purchase_option?.ifEmpty { null }
    }
}

fun PermissionsDto.toSearchPermissionsEntity() : PermissionSettingsEntity{
    val dto = this
    return PermissionSettingsEntity().apply {
        // Search permissions behavior only
        // Default to HIDE when not defined.
        behavior = dto.search_permissions_behavior?.ifEmpty { null } ?: PermissionBehavior.HIDE.value
        alternatePageId = dto.search_permissions_alternate_page_id?.ifEmpty { null }
        secondaryMarketPurchaseOption =
            dto.search_permissions_secondary_market_purchase_option?.ifEmpty { null }
    }
}
