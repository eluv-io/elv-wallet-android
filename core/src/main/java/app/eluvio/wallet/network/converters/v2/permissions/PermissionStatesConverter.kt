package app.eluvio.wallet.network.converters.v2.permissions

import app.eluvio.wallet.data.entities.v2.permissions.PermissionStatesEntity
import app.eluvio.wallet.network.dto.v2.permissions.PermissionStateHolder
import app.eluvio.wallet.util.realm.toRealmDictionaryOrEmpty
import io.realm.kotlin.types.RealmDictionary

fun PermissionStateHolder.toPermissionStateEntities(): RealmDictionary<PermissionStatesEntity?> {
    return permissionStates?.mapValues { (permissionItemId, state) ->
        PermissionStatesEntity().apply {
            id = permissionItemId
            authorized = state.authorized
        }
    }.toRealmDictionaryOrEmpty()
}
