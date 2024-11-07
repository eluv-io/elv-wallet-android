package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.DisplayFormat

interface DisplaySettings {
    val title: String?
    val subtitle: String?
    val headers: List<String>
    val description: String?

    val forcedAspectRatio: Float?
    val thumbnailLandscapeUrl: FabricUrl?
    val thumbnailPortraitUrl: FabricUrl?
    val thumbnailSquareUrl: FabricUrl?

    val displayLimit: Int?
    val displayLimitType: String?
    val displayFormat: DisplayFormat

    // For Sections: Shows to the left of first item
    val logoUrl: FabricUrl?
    val logoText: String?

    // Hex color
    val inlineBackgroundColor: String?
    val inlineBackgroundImageUrl: FabricUrl?

    val heroBackgroundImageUrl: FabricUrl?
    val heroBackgroundVideoHash: String?

    val hiddenOnTv: Boolean?
}

/**
 * Returns the best thumbnail path and aspect ratio, based on the forced aspect ratio.
 */
val DisplaySettings.thumbnailUrlAndRatio: Pair<String, Float>?
    get() {
        // First available, if no forced aspect ratio
        val default = thumbnailSquareUrl?.url?.let { it to AspectRatio.SQUARE }
            ?: thumbnailPortraitUrl?.url?.let { it to AspectRatio.POSTER }
            ?: thumbnailLandscapeUrl?.url?.let { it to AspectRatio.WIDE }
        return forcedAspectRatio?.let { ratio ->
            // If aspect ratio is defined, prefer the corresponding thumbnail, but force aspect
            // ratio regardless of whether a matching thumbnail exists or not.
            val matchingUrl = when (ratio) {
                AspectRatio.SQUARE -> thumbnailSquareUrl?.url
                AspectRatio.POSTER -> thumbnailPortraitUrl?.url
                AspectRatio.WIDE -> thumbnailLandscapeUrl?.url
                else -> null
            }
            (matchingUrl ?: default?.first)?.let { it to ratio }
        } ?: default
    }

// Basic implementation of DisplaySettings, to be used for on-the-fly, un-persisted settings.
data class SimpleDisplaySettings(
    override val title: String? = null,
    override val subtitle: String? = null,
    override val headers: List<String> = emptyList(),
    override val description: String? = null,
    override val forcedAspectRatio: Float? = null,
    override val thumbnailLandscapeUrl: FabricUrl? = null,
    override val thumbnailPortraitUrl: FabricUrl? = null,
    override val thumbnailSquareUrl: FabricUrl? = null,
    override val displayLimit: Int? = null,
    override val displayLimitType: String? = null,
    override val displayFormat: DisplayFormat = DisplayFormat.UNKNOWN,
    override val logoUrl: FabricUrl? = null,
    override val logoText: String? = null,
    override val inlineBackgroundColor: String? = null,
    override val inlineBackgroundImageUrl: FabricUrl? = null,
    override val heroBackgroundImageUrl: FabricUrl? = null,
    override val heroBackgroundVideoHash: String? = null,
    override val hiddenOnTv: Boolean? = null,
) : DisplaySettings {
    companion object {
        fun from(other: DisplaySettings?, forcedAspectRatio: Float? = null): SimpleDisplaySettings {
            return SimpleDisplaySettings(
                title = other?.title,
                subtitle = other?.subtitle,
                headers = other?.headers ?: emptyList(),
                description = other?.description,
                forcedAspectRatio = forcedAspectRatio ?: other?.forcedAspectRatio,
                thumbnailLandscapeUrl = other?.thumbnailLandscapeUrl,
                thumbnailPortraitUrl = other?.thumbnailPortraitUrl,
                thumbnailSquareUrl = other?.thumbnailSquareUrl,
                displayLimit = other?.displayLimit,
                displayLimitType = other?.displayLimitType,
                displayFormat = other?.displayFormat ?: DisplayFormat.UNKNOWN,
                logoUrl = other?.logoUrl,
                logoText = other?.logoText,
                inlineBackgroundColor = other?.inlineBackgroundColor,
                inlineBackgroundImageUrl = other?.inlineBackgroundImageUrl,
                heroBackgroundImageUrl = other?.heroBackgroundImageUrl,
                heroBackgroundVideoHash = other?.heroBackgroundVideoHash,
                hiddenOnTv = other?.hiddenOnTv,
            )
        }
    }
}

fun DisplaySettings.withOverrides(overrides: DisplaySettings?): DisplaySettings {
    val default = this
    overrides ?: return default

    return SimpleDisplaySettings(
        title = overrides.title?.ifEmpty { null } ?: default.title,
        subtitle = overrides.subtitle?.ifEmpty { null } ?: default.subtitle,
        headers = overrides.headers.ifEmpty { default.headers },
        description = overrides.description?.ifEmpty { null } ?: default.description,
        forcedAspectRatio = overrides.forcedAspectRatio ?: default.forcedAspectRatio,
        thumbnailLandscapeUrl = overrides.thumbnailLandscapeUrl ?: default.thumbnailLandscapeUrl,
        thumbnailPortraitUrl = overrides.thumbnailPortraitUrl ?: default.thumbnailPortraitUrl,
        thumbnailSquareUrl = overrides.thumbnailSquareUrl ?: default.thumbnailSquareUrl,
        displayLimit = overrides.displayLimit ?: default.displayLimit,
        displayLimitType = overrides.displayLimitType?.ifEmpty { null } ?: default.displayLimitType,
        displayFormat = overrides.displayFormat.takeIf { it != DisplayFormat.UNKNOWN }
            ?: default.displayFormat,
        logoUrl = overrides.logoUrl ?: default.logoUrl,
        logoText = overrides.logoText?.ifEmpty { null } ?: default.logoText,
        inlineBackgroundColor = overrides.inlineBackgroundColor?.ifEmpty { null }
            ?: default.inlineBackgroundColor,
        inlineBackgroundImageUrl = overrides.inlineBackgroundImageUrl
            ?: default.inlineBackgroundImageUrl,
        heroBackgroundImageUrl = overrides.heroBackgroundImageUrl ?: default.heroBackgroundImageUrl,
        heroBackgroundVideoHash = overrides.heroBackgroundVideoHash ?: default.heroBackgroundVideoHash,
        hiddenOnTv = overrides.hiddenOnTv ?: default.hiddenOnTv,
    )
}
