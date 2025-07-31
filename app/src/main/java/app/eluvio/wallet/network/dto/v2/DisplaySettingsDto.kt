package app.eluvio.wallet.network.dto.v2

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.PlayableHashDto

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
    val background_video: PlayableHashDto?

    val hide_on_tv: Boolean?

    /** Applies to "banner" items that should be displayed edge-to-edge on the screen. */
    val full_bleed: Boolean?
}
