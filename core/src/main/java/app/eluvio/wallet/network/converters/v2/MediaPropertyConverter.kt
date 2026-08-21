package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertyLoginInfoRealmEntity
import app.eluvio.wallet.data.entities.v2.display.CardBorderRadius
import app.eluvio.wallet.data.entities.v2.display.CardEffect
import app.eluvio.wallet.data.entities.v2.display.CardThemeEntity
import app.eluvio.wallet.data.entities.v2.display.CardThemeStateEntity
import app.eluvio.wallet.network.converters.v2.permissions.toContentPermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toPagePermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toPermissionStateEntities
import app.eluvio.wallet.network.converters.v2.permissions.toPropertyPermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toSearchPermissionsEntity
import app.eluvio.wallet.network.dto.v2.CardThemeDto
import app.eluvio.wallet.network.dto.v2.CardThemeStateDto
import app.eluvio.wallet.network.dto.v2.LoginInfoDto
import app.eluvio.wallet.network.dto.v2.MediaPageDto
import app.eluvio.wallet.network.dto.v2.MediaPropertyDto
import app.eluvio.wallet.network.dto.v2.PropertySelectionDto
import app.eluvio.wallet.util.realm.toRealmDictionaryOrEmpty
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.realm.kotlin.ext.toRealmList

fun MediaPropertyDto.toEntity(baseUrl: String): MediaPropertyEntity? {
    val dto = this
    return MediaPropertyEntity().apply {
        id = dto.id
        name = dto.title?.ifEmpty { null } ?: dto.name
        headerLogoUrl = (dto.tvHeaderLogo ?: dto.headerLogo)?.toUrl(baseUrl)
        // We can't handle properties without images
        image = dto.image?.toUrl(baseUrl, dto.image_hash) ?: return null
        featuredImage = dto.featuredImage?.toUrl(baseUrl, dto.featuredImageHash)
        // No ThumbHash here on purpose, see [MediaPropertyDto.mainPageLogo].
        featuredCardLogo = dto.mainPageLogo?.toUrl(baseUrl)
        bgImageUrl = dto.discoverPageBgImage?.toUrl(baseUrl, dto.discoverPageBgImageHash)
        // Stand-in field, see [MediaPropertyDto.mainPageCardVideo].
        heroVideoHash = dto.mainPageCardVideo?.hash
        mainPageTitle = dto.mainPageTitle?.ifBlank { null }
        mainPageDescription = dto.mainPageDescription?.ifBlank { null }
        mainPage = dto.mainPage.toEntity(id, baseUrl)
        subpropertySelection = dto.property_selection
            .takeIf { dto.show_property_selection == true }
            ?.map { it.toEntity(baseUrl) }
            .toRealmListOrEmpty()

        loginInfo = dto.login?.toEntity(baseUrl)
        tenantId = dto.tenant?.tenant_iten ?: dto.tenant?.id

        startScreenBackground = dto.start_screen_background?.toUrl(baseUrl)
        startScreenLogo = dto.start_screen_logo?.toUrl(baseUrl)

        countdownBackground = dto.countdown_background_desktop?.toUrl(baseUrl)

        cardThemeId = dto.card_theme_id?.ifEmpty { null }
        cardThemes = dto.styling?.card_themes
            ?.mapValues { (themeId, theme) -> theme.toEntity(themeId) }
            .toRealmDictionaryOrEmpty()

        permissionStates = dto.toPermissionStateEntities()
        rawPermissions = dto.permissions?.toContentPermissionsEntity()
        propertyPermissions = dto.permissions?.toPropertyPermissionsEntity()
        searchPermissions = dto.permissions?.toSearchPermissionsEntity()
    }
}

private fun PropertySelectionDto.toEntity(baseUrl: String): MediaPropertyEntity.SubpropertySelectionEntity {
    val dto = this
    return MediaPropertyEntity.SubpropertySelectionEntity().apply {
        id = dto.property_id
        title = dto.title
        icon = dto.icon?.toUrl(baseUrl)
        tile = dto.tile?.toUrl(baseUrl)
    }
}

private fun LoginInfoDto.toEntity(baseUrl: String): PropertyLoginInfoRealmEntity {
    val dto = this
    return PropertyLoginInfoRealmEntity().apply {
        backgroundImageUrl =
            (dto.styling?.backgroundImageTv ?: dto.styling?.backgroundImageDesktop)?.toUrl(baseUrl)
        logoUrl = (dto.styling?.logoTv ?: dto.styling?.logo)?.toUrl(baseUrl)
        loginProvider = if (dto.settings?.use_auth0 == true) "auth0_${dto.settings.auth0_domain}" else "ory"
        skipLogin = dto.settings?.disable_login == true
    }
}

private fun CardThemeDto.toEntity(themeId: String): CardThemeEntity {
    val dto = this
    return CardThemeEntity().apply {
        id = dto.id?.ifEmpty { null } ?: themeId
        borderRadius = CardBorderRadius.from(dto.border_radius)
        borderWidth = dto.border_width ?: 0
        circularize = dto.circularize == true
        effect = CardEffect.from(dto.effect)
        active = dto.active?.toEntity()
        inactive = dto.inactive?.toEntity()
    }
}

private fun CardThemeStateDto.toEntity(): CardThemeStateEntity {
    val dto = this
    return CardThemeStateEntity().apply {
        borderColor = dto.border_color?.ifEmpty { null }
        backgroundColor = dto.background_color?.ifEmpty { null }
        backgroundColorOpacity = dto.background_color_opacity ?: FULLY_OPAQUE
        backgroundColor2 = dto.background_color_2?.ifEmpty { null }
        backgroundColor2Opacity = dto.background_color_2_opacity ?: FULLY_OPAQUE
        backgroundGradientAngle = dto.background_gradient_angle ?: 0
        gradient = dto.background_type == "gradient"
    }
}

/** Opacities are percentages, and a missing one means the color is fully opaque. */
private const val FULLY_OPAQUE = 100

fun MediaPageDto.toEntity(propertyId: String, baseUrl: String): MediaPageEntity {
    val dto = this
    val layout = dto.layout
    return MediaPageEntity().apply {
        // Page ID's aren't unique across properties (but they should be), so as a workaround we use the property ID as a prefix
        uid = MediaPageEntity.uid(propertyId, dto.id)
        id = dto.id
        sectionIds = layout.sections.toRealmList()
        backgroundImageUrl = layout.backgroundImage?.toUrl(baseUrl, layout.backgroundImageHash)
        logoUrl = layout.logo?.toUrl(baseUrl)
        title = layout.title
        description = layout.description
        descriptionRichText = layout.descriptionRichText
        cardThemeId = dto.card_theme_id?.ifEmpty { null }
        rawPermissions = dto.permissions?.toContentPermissionsEntity()
        pagePermissions = dto.permissions?.toPagePermissionsEntity()
    }
}
