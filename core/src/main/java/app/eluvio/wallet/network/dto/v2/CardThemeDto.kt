package app.eluvio.wallet.network.dto.v2

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StylingDto(
    val card_themes: Map<String, CardThemeDto>?,
)

/**
 * Defines the visuals of a card. Referenced by [MediaPropertyDto.card_theme_id],
 * [MediaPageDto.card_theme_id] and [DisplaySettingsDto.card_theme_id].
 */
@JsonClass(generateAdapter = true)
data class CardThemeDto(
    val id: String?,
    // none/subtle/curved
    val border_radius: String?,
    val border_width: Int?,
    val circularize: Boolean?,
    // Singular. There's also an "effects" object in some payloads, but the web reads this one.
    val effect: String?,
    val active: CardThemeStateDto?,
    val inactive: CardThemeStateDto?,
)

@JsonClass(generateAdapter = true)
data class CardThemeStateDto(
    val border_color: String?,
    // solid/gradient. Anything but "gradient" means [background_color] fills the whole card.
    val background_type: String?,
    val background_color: String?,
    /** Percent, 0-100. Missing means fully opaque. */
    val background_color_opacity: Int?,
    val background_color_2: String?,
    val background_color_2_opacity: Int?,
    /** Degrees, clockwise from "up", like a CSS gradient. */
    val background_gradient_angle: Int?,
)
