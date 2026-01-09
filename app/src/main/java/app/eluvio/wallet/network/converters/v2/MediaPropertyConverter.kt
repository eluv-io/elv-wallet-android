package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertyLoginInfoRealmEntity
import app.eluvio.wallet.network.converters.v2.permissions.toContentPermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toPagePermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toPermissionStateEntities
import app.eluvio.wallet.network.converters.v2.permissions.toPropertyPermissionsEntity
import app.eluvio.wallet.network.converters.v2.permissions.toSearchPermissionsEntity
import app.eluvio.wallet.network.dto.v2.LoginInfoDto
import app.eluvio.wallet.network.dto.v2.MediaPageDto
import app.eluvio.wallet.network.dto.v2.MediaPropertyDto
import app.eluvio.wallet.network.dto.v2.PropertySelectionDto
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.realm.kotlin.ext.toRealmList

fun MediaPropertyDto.toEntity(baseUrl: String): MediaPropertyEntity? {
    val dto = this
    return MediaPropertyEntity().apply {
        id = dto.id
        name = dto.title?.ifEmpty { null } ?: dto.name
        headerLogoUrl = (dto.tvHeaderLogo ?: dto.headerLogo)?.toUrl(baseUrl)
        // We can't handle properties without images
        image = dto.image?.toUrl(baseUrl) ?: return null
        bgImageUrl = dto.discoverPageBgImage?.toUrl(baseUrl)
        mainPage = dto.mainPage.toEntity(id, baseUrl)
        subpropertySelection = dto.property_selection
            .takeIf { dto.show_property_selection == true }
            ?.map { it.toEntity(baseUrl) }
            .toRealmListOrEmpty()

        loginInfo = dto.login?.toEntity(baseUrl)
        tenantId = dto.tenant?.id

        startScreenBackground = dto.start_screen_background?.toUrl(baseUrl)
        startScreenLogo = dto.start_screen_logo?.toUrl(baseUrl)

        countdownBackground = dto.countdown_background_desktop?.toUrl(baseUrl)

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

fun MediaPageDto.toEntity(propertyId: String, baseUrl: String): MediaPageEntity {
    val dto = this
    val layout = dto.layout
    return MediaPageEntity().apply {
        // Page ID's aren't unique across properties (but they should be), so as a workaround we use the property ID as a prefix
        uid = MediaPageEntity.uid(propertyId, dto.id)
        id = dto.id
        sectionIds = layout.sections.toRealmList()
        backgroundImageUrl = layout.backgroundImage?.toUrl(baseUrl)
        logoUrl = layout.logo?.toUrl(baseUrl)
        title = layout.title
        description = layout.description
        descriptionRichText = layout.descriptionRichText
        rawPermissions = dto.permissions?.toContentPermissionsEntity()
        pagePermissions = dto.permissions?.toPagePermissionsEntity()
    }
}
