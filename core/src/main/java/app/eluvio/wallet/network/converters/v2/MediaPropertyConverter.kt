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
import app.eluvio.wallet.network.dto.v2.LoginSettingsDto
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
        heroVideoHash = dto.heroVideo?.hash
        mainPageTitle = dto.mainPageTitle?.ifBlank { null }
        mainPageDescription = dto.mainPageDescription?.ifBlank { null }
        mainPageInaccessible = dto.mainPageInaccessible == true
        mainPageInaccessibleMessage = dto.mainPageInaccessibleMessage?.ifBlank { null }
        mainPage = dto.mainPage.toEntity(id, baseUrl)
        subpropertySelection = dto.property_selection
            .takeIf { dto.show_property_selection == true }
            ?.map { it.toEntity(baseUrl) }
            .toRealmListOrEmpty()

        loginInfo = dto.login?.toEntity(baseUrl)
        tvLoginCustomDomain = dto.domain?.tvLoginCustomDomain?.ifBlank { null }
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
        searchPrimaryFilterStyle = dto.search?.primaryFilterStyle?.ifEmpty { null }
        searchPrimaryFilterCardThemeId = dto.search?.primaryFilterCardThemeId?.ifEmpty { null }
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
        loginProvider = dto.settings.toLoginProvider()
        skipLogin = dto.settings?.disable_login == true
    }
}

/**
 * Identifies which login provider a Property uses, so we can tell whether an existing session is
 * usable for it. The value is opaque - we only ever compare it to another Property's.
 *
 * [LoginSettingsDto.provider_id] is the source of truth, but it's still rolling out, so until
 * every Property has one, its absence means "unknown" rather than "ory", and we derive the
 * equivalent string from the per-provider fields ourselves. Auth0/OpenID sessions are only shared
 * between Properties pointing at the same domain/endpoint, so that's encoded into the string.
 * Like the web client, a provider flag only counts when its domain/endpoint is set too.
 *
 * Once the rollout is complete, everything below the [provider_id] branch can be deleted, along
 * with the per-provider fields on [LoginSettingsDto].
 */
private fun LoginSettingsDto?.toLoginProvider(): String = when {
    this == null -> "ory"
    !provider_id.isNullOrEmpty() -> provider_id
    use_auth0 == true && !auth0_domain.isNullOrEmpty() -> "auth0_$auth0_domain"
    use_openid == true && !openid_endpoint.isNullOrEmpty() -> "openid_$openid_endpoint"
    else -> "ory"
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
