package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.PlayableHashDto
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
    val image_hash: String?,
    // Logo drawn on top of the Property's card in a featured Discover row.
    // The server also sends a "main_page_logo_hash" (ThumbHash) alongside this, which we
    // deliberately don't parse: these logos are transparent PNGs, and a blurred ThumbHash
    // placeholder behind one looks like a smudge until the real image lands.
    @field:Json(name = "main_page_logo")
    val mainPageLogo: AssetLinkDto? = null,
    // Portrait art for the Property's card in a featured Discover row.
    @field:Json(name = "featured_image")
    val featuredImage: AssetLinkDto? = null,
    @field:Json(name = "featured_image_hash")
    val featuredImageHash: String? = null,
    @field:Json(name = "image_tv")
    val discoverPageBgImage: AssetLinkDto?,
    @field:Json(name = "image_tv_hash")
    val discoverPageBgImageHash: String? = null,
    // The Discover hero video. No Property sets this yet.
    @field:Json(name = "main_page_background_video_tv")
    val heroVideo: PlayableHashDto? = null,
    val name: String,
    val title: String?,
    @field:Json(name = "main_page")
    val mainPage: MediaPageDto,
    // Title/description for this Property on the Discover ("main") page.
    @field:Json(name = "main_page_title")
    val mainPageTitle: String?,
    @field:Json(name = "main_page_description")
    val mainPageDescription: String?,
    val show_property_selection: Boolean?,
    val property_selection: List<PropertySelectionDto>?,

    val login: LoginInfoDto?,

    val tenant: TenantDto?,

    val card_theme_id: String?,
    val styling: StylingDto?,

    // For single-property custom builds
    val start_screen_background: AssetLinkDto?,
    val start_screen_logo: AssetLinkDto?,

    val countdown_background_desktop: AssetLinkDto?,

    // For each permission used in the property, holds whether or not the user is authorized for it.
    @field:Json(name = "permission_auth_state")
    override val permissionStates: Map<String, PermissionsStateDto>?,

    override val permissions: PermissionsDto?,
) : DtoWithPermissions, PermissionStateHolder

@JsonClass(generateAdapter = true)
data class TenantDto(
    // This is actually the "tenant object id" (iq__...)
    @field:Json(name = "tenant_id")
    val id: String,
    // The real tenant id (iten...)
    val tenant_iten: String?,
)

@JsonClass(generateAdapter = true)
data class LoginInfoDto(
    val settings: LoginSettingsDto?,
    val styling: LoginStylingDto?,
)

@JsonClass(generateAdapter = true)
data class LoginSettingsDto(
    val use_auth0: Boolean?,
    val disable_login: Boolean?,
    val auth0_domain: String?,
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
