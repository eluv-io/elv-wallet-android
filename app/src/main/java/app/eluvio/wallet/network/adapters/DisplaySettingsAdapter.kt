package app.eluvio.wallet.network.adapters

import app.eluvio.wallet.network.dto.AssetLinkDto
import app.eluvio.wallet.network.dto.PlayableHashDto
import app.eluvio.wallet.network.dto.v2.DisplaySettingsDto
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson

/**
 * Converts incoming [DisplaySettingsDto] into a basic data class implementation of the interface.
 * Does not support converting back to JSON.
 */
class DisplaySettingsAdapter {
    @FromJson
    fun fromJson(impl: DisplaySettingsDtoImpl): DisplaySettingsDto {
        return impl
    }

    @ToJson
    fun toJson(dto: DisplaySettingsDto): DisplaySettingsDtoImpl {
        throw UnsupportedOperationException("Can't convert DisplaySettingsDto to JSON")
    }
}

@JsonClass(generateAdapter = true)
data class DisplaySettingsDtoImpl(
    override val title: String?,
    override val subtitle: String?,
    override val headers: List<String>?,
    override val description: String?,
    override val aspect_ratio: String?,
    override val thumbnail_image_landscape: AssetLinkDto?,
    override val thumbnail_image_portrait: AssetLinkDto?,
    override val thumbnail_image_square: AssetLinkDto?,

    override val display_limit: Int?,
    override val display_limit_type: String?,
    override val display_format: String?,

    override val logo: AssetLinkDto?,
    override val logo_text: String?,
    override val inline_background_color: String?,
    override val inline_background_image: AssetLinkDto?,

    override val background_image: AssetLinkDto?,
    override val background_video: PlayableHashDto?,

    override val hide_on_tv: Boolean?,
) : DisplaySettingsDto
