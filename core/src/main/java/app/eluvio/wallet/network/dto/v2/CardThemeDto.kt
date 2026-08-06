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
    val active: CardThemeStateDto?,
    val inactive: CardThemeStateDto?,
)

@JsonClass(generateAdapter = true)
data class CardThemeStateDto(
    val border_color: String?,
)
