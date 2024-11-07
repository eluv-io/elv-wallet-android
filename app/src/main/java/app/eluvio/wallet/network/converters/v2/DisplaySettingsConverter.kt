package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.display.DisplaySettingsEntity
import app.eluvio.wallet.network.dto.v2.DisplaySettingsDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty

fun DisplaySettingsDto.toEntity(baseUrl: String): DisplaySettingsEntity {
    val dto = this
    return DisplaySettingsEntity().apply {
        title = dto.title?.ifEmpty { null }
        subtitle = dto.subtitle?.ifEmpty { null }
        headers = dto.headers.toRealmListOrEmpty()
        description = dto.description?.ifEmpty { null }
        forcedAspectRatio = AspectRatio.parse(dto.aspect_ratio)
        thumbnailLandscapeUrl = dto.thumbnail_image_landscape?.toUrl(baseUrl)
        thumbnailPortraitUrl = dto.thumbnail_image_portrait?.toUrl(baseUrl)
        thumbnailSquareUrl = dto.thumbnail_image_square?.toUrl(baseUrl)
        displayLimit = dto.display_limit?.takeIf {
            // Display limit of 0 means no limit. Which is the same handling we do for null, so just
            // turn zeros to null.
            it > 0
        }
        displayLimitType = dto.display_limit_type?.ifEmpty { null }
        displayFormat = DisplayFormat.from(dto.display_format)
        logoUrl = dto.logo?.toUrl(baseUrl)
        logoText = dto.logo_text?.ifEmpty { null }
        inlineBackgroundColor = dto.inline_background_color?.ifEmpty { null }
        inlineBackgroundImageUrl = dto.inline_background_image?.toUrl(baseUrl)
        heroBackgroundImageUrl = dto.background_image?.toUrl(baseUrl)
        heroBackgroundVideoHash = dto.background_video?.hash
        hiddenOnTv = dto.hide_on_tv
    }
}
