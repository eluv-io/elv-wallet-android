package app.eluvio.wallet.network.dto.v2.permissions

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetPermissionResponse(
    @field:Json(name = "permission_auth_state")
    override val permissionStates: Map<String, PermissionsStateDto>?,
) : PermissionStateHolder
