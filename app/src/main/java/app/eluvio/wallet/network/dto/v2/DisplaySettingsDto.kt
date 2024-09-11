package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto

@Suppress("PropertyName")
interface DisplaySettingsDto {
    val title: String?
    val subtitle: String?
    val headers: List<String>?
    val description: String?
    val aspect_ratio: String?
    val thumbnail_image_landscape: AssetLinkDto?
    val thumbnail_image_portrait: AssetLinkDto?
    val thumbnail_image_square: AssetLinkDto?

    val display_limit: Int?
    val display_limit_type: String?
    val display_format: String?

    val logo: AssetLinkDto?
    val logo_text: String?
    val inline_background_color: String?
    val inline_background_image: AssetLinkDto?

    val background_image: AssetLinkDto?
}
