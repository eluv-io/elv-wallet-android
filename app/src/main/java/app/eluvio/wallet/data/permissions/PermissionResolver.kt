package app.eluvio.wallet.data.permissions

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettings
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionStatesEntity
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import app.eluvio.wallet.data.permissions.PermissionResolver.resolvePermissions
import app.eluvio.wallet.util.logging.Log

object PermissionResolver {
    /**
     * Recursively resolves permissions and updates entities with resolved permissions.
     */
    fun resolvePermissions(
        entity: EntityWithPermissions,
        parentPermissions: PermissionSettings?,
        permissionStates: Map<String, PermissionStatesEntity?>
    ) {
        // Special cases
        if (resolveSpecialPermissions(entity, permissionStates)) {
            Log.w("Short-circuiting content permission resolution for $entity")
            return
        }

        resolveContentPermissions(entity, parentPermissions, permissionStates)

        // Iterate children and update their permissions too
        entity.permissionChildren.forEach { child ->
            resolvePermissions(child, entity.resolvedPermissions, permissionStates)
        }
    }

    /**
     * Convenience method to resolve permissions for a list of entities.
     * @see resolvePermissions
     */
    fun resolvePermissions(
        entities: List<EntityWithPermissions>,
        parentPermissions: PermissionSettings?,
        permissionStates: Map<String, PermissionStatesEntity?>
    ) {
        entities.forEach { entity ->
            resolvePermissions(entity, parentPermissions, permissionStates)
        }
    }

    /**
     * Updates [entity] with resolved permissions.
     */
    private fun resolveContentPermissions(
        entity: EntityWithPermissions,
        parentPermissions: PermissionSettings?,
        permissionStates: Map<String, PermissionStatesEntity?>
    ) {
        entity.resolvedPermissions =
            if (parentPermissions == null) {
                // top level, resolve with [entity] as parent.
                entity.rawPermissions?.let {
                    merge(
                        parent = it,
                        child = null,
                        permissionStates = permissionStates
                    ).asVolatile()
                }
            } else {
                merge(
                    parent = parentPermissions,
                    child = entity.rawPermissions,
                    permissionStates = permissionStates
                ).asVolatile()
            }
    }

    /**
     * @return true if we can short-circuit the content permission resolution process.
     */
    private fun resolveSpecialPermissions(
        entity: EntityWithPermissions,
        permissionStates: Map<String, PermissionStatesEntity?>
    ): Boolean {
        if (entity is MediaPropertyEntity) {
            entity.propertyPermissions?.let {
                entity.propertyPermissions = merge(
                    parent = it,
                    child = null,
                    permissionStates = permissionStates
                ).asEntity()
            }
            // An in-accessible property could still render a Page, so we can't short-circuit here.
            return false
        } else if (entity is MediaPageEntity) {
            entity.pagePermissions?.let {
                entity.pagePermissions = merge(
                    parent = it,
                    child = null,
                    permissionStates = permissionStates
                ).asEntity()
            }
            // In the case of an unauthorized page, we can save ourselves from checking any content
            // permissions, because none of that content will be visible to the user
            return entity.pagePermissions?.authorized == false
        }
        return false
    }

    /**
     * Returns a new [CompositePermissions] with merged permissions.
     * Note that [parent] and [child] are not treated equally, and parent permissions take over
     * once we hit an unauthorized state.
     */
    private fun merge(
        parent: PermissionSettings,
        child: PermissionSettings?,
        permissionStates: Map<String, PermissionStatesEntity?>,
    ): CompositePermissions {
        return when {
            child == null -> {
                // Child has nothing defined, [authorized] will be the same as the parent's,
                // unless it still needs to be calculated.
                CompositePermissions(
                    authorized = parent.authorized ?: parent.calcAuthorized(permissionStates),
                    primary = parent,
                    fallback = parent
                )
            }

            parent.authorized == false -> {
                // Parent is not authorized, everything down the line is not authorized
                // and will inherit behavior, unless it's not already set.
                CompositePermissions(
                    authorized = false,
                    primary = parent,
                    fallback = child
                )
            }

            else -> {
                // Parent is authorized, child will have to check its own permissions.
                // Child fields take precedence over parent fields.
                CompositePermissions(
                    authorized = child.calcAuthorized(permissionStates),
                    primary = child,
                    fallback = parent
                )
            }
        }
    }
}

/**
 * Returns true if any of the [PermissionSettings.permissionItemIds] are authorized.
 * If [PermissionSettings.permissionItemIds] is empty, also returns true, since it means there is
 * no item requirement for access.
 */
private fun PermissionSettings.calcAuthorized(permissionStates: Map<String, PermissionStatesEntity?>): Boolean {
    return permissionItemIds.isEmpty() || permissionItemIds.any { permissionStates[it]?.authorized == true }
}

/**
 * Tries to delegate to [primary] first, and falls back to [fallback] if [primary] is not defined.
 * While it's only used once and doesn't save us much code, the real benefit of this class is to make
 * sure we get a compilation error any time [PermissionSettings] is updated, so we don't forget to
 * include the new field in the permission resolution logic.
 */
private class CompositePermissions(
    override val authorized: Boolean,
    private val primary: PermissionSettings,
    private val fallback: PermissionSettings
) : PermissionSettings {
    override val permissionItemIds: List<String>
        get() = primary.permissionItemIds.takeIf { it.isNotEmpty() } ?: fallback.permissionItemIds
    override val behavior: String?
        get() = primary.behavior ?: fallback.behavior
    override val alternatePageId: String?
        get() = primary.alternatePageId ?: fallback.alternatePageId
    override val secondaryMarketPurchaseOption: String?
        get() = primary.secondaryMarketPurchaseOption ?: fallback.secondaryMarketPurchaseOption

    fun asEntity(): PermissionSettingsEntity {
        val settings = this
        return PermissionSettingsEntity().apply {
            authorized = settings.authorized
            permissionItemIds = settings.permissionItemIds
            behavior = settings.behavior
            alternatePageId = settings.alternatePageId
            secondaryMarketPurchaseOption = settings.secondaryMarketPurchaseOption
        }
    }

    fun asVolatile(): VolatilePermissionSettings {
        return VolatilePermissionSettings(
            authorized = authorized,
            permissionItemIds = permissionItemIds,
            behavior = behavior,
            alternatePageId = alternatePageId,
            secondaryMarketPurchaseOption = secondaryMarketPurchaseOption
        )
    }
}
