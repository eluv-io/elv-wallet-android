package app.eluvio.wallet.network.converters.v2

import app.eluvio.wallet.data.entities.v2.LoginProviders
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
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.realm.kotlin.ext.toRealmList

fun MediaPropertyDto.toEntity(baseUrl: String): MediaPropertyEntity? {
    val dto = this
    return MediaPropertyEntity().apply {
        id = dto.id
        name = dto.name
        headerLogoUrl = (dto.tvHeaderLogo ?: dto.headerLogo)?.toUrl(baseUrl)
        // We can't handle properties without images
        image = dto.image?.toUrl(baseUrl) ?: return null
        bgImageUrl = dto.discoverPageBgImage?.toUrl(baseUrl)
        mainPage = dto.mainPage.toEntity(id, baseUrl)
        subproperyIds = dto.subpropertyIds.toRealmListOrEmpty()

        loginInfo = dto.login?.toEntity(baseUrl)

        permissionStates = dto.toPermissionStateEntities()
        rawPermissions = dto.permissions?.toContentPermissionsEntity()
        propertyPermissions = dto.permissions?.toPropertyPermissionsEntity()
        searchPermissions = dto.permissions?.toSearchPermissionsEntity()
    }
}

private fun LoginInfoDto.toEntity(baseUrl: String): PropertyLoginInfoRealmEntity {
    val dto = this
    return PropertyLoginInfoRealmEntity().apply {
        backgroundImageUrl =
            (dto.styling?.backgroundImageTv ?: dto.styling?.backgroundImageDesktop)?.toUrl(baseUrl)
        logoUrl = (dto.styling?.logoTv ?: dto.styling?.logo)?.toUrl(baseUrl)
        loginProvider = LoginProviders.from(dto.settings?.provider)
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
