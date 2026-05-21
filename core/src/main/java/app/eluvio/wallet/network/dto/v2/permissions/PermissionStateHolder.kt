package app.eluvio.wallet.network.dto.v2.permissions

/**
 * Any object that holds permission states.
 * This is useful to re-use logic when parsing Properties, or fetching PermissionStates separately.
 */
interface PermissionStateHolder {
    val permissionStates: Map<String, PermissionsStateDto>?
}
