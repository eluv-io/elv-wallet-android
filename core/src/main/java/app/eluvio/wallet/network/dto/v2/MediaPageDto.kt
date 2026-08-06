package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.v2.permissions.DtoWithPermissions
import app.eluvio.wallet.network.dto.v2.permissions.PermissionsDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaPageDto(
    val description: String?,
    val id: String,
    val label: String,
    val layout: PageLayoutDto,
    val card_theme_id: String?,
    override val permissions: PermissionsDto?,
) : DtoWithPermissions

@JsonClass(generateAdapter = true)
data class PageLayoutDto(
    @field:Json(name = "background_image")
    val backgroundImage: AssetLinkDto?,
    @field:Json(name = "background_image_hash")
    val backgroundImageHash: String?,
    val title: String?,
    val description: String?,
    @field:Json(name = "description_rich_text")
    val descriptionRichText: String?,
    val logo: AssetLinkDto?,
    @field:Json(name = "logo_alt")
    val logoAlt: String?,
    // Align logo and text to: Left/Right/Center
    val position: String?,
    // List of section IDs.
    val sections: List<String>,
)
