package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.v2.permissions.DtoWithPermissions
import app.eluvio.wallet.network.dto.v2.permissions.PermissionStateHolder
import app.eluvio.wallet.network.dto.v2.permissions.PermissionsDto
import app.eluvio.wallet.network.dto.v2.permissions.PermissionsStateDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaPropertyDto(
    val description: String?,
    @field:Json(name = "header_logo")
    val headerLogo: AssetLinkDto?,
    @field:Json(name = "tv_header_logo")
    val tvHeaderLogo: AssetLinkDto?,
    val id: String,
    val image: AssetLinkDto?,
    @field:Json(name = "image_tv")
    val discoverPageBgImage: AssetLinkDto?,
    val name: String,
    val title: String?,
    @field:Json(name = "main_page")
    val mainPage: MediaPageDto,
    val show_property_selection: Boolean?,
    val property_selection: List<PropertySelectionDto>?,

    val login: LoginInfoDto?,

    // For each permission used in the property, holds whether or not the user is authorized for it.
    @field:Json(name = "permission_auth_state")
    override val permissionStates: Map<String, PermissionsStateDto>?,

    override val permissions: PermissionsDto?,
) : DtoWithPermissions, PermissionStateHolder

@JsonClass(generateAdapter = true)
data class LoginInfoDto(
    val settings: LoginSettingsDto?,
    val styling: LoginStylingDto?,
)

@JsonClass(generateAdapter = true)
data class LoginSettingsDto(
    val provider: String?,
    val disable_login: Boolean?,
)

@JsonClass(generateAdapter = true)
data class LoginStylingDto(

    @field:Json(name = "background_image_tv")
    val backgroundImageTv: AssetLinkDto?,
    @field:Json(name = "background_image_desktop")
    val backgroundImageDesktop: AssetLinkDto?,

    @field:Json(name = "logo_tv")
    val logoTv: AssetLinkDto?,
    val logo: AssetLinkDto?
)

@JsonClass(generateAdapter = true)
data class PropertySelectionDto(
    val property_id: String,
    val title: String?,
    val icon: AssetLinkDto?,
    val tile: AssetLinkDto?,
)
