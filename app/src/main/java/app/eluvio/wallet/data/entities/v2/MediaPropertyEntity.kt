package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionStatesEntity
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class MediaPropertyEntity : RealmObject, EntityWithPermissions {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var headerLogoUrl: FabricUrlEntity? = null

    // Poster image used on Discover page
    var image: FabricUrlEntity? = null

    // Background image used on Discover page when the Property is selected
    var bgImageUrl: FabricUrlEntity? = null
    val bgImageWithFallback: FabricUrl?
        get() = bgImageUrl ?: mainPage?.backgroundImageUrl

    // Property can also include a list of pages besides the main page.
    // But the TV apps have no use for it currently.
    var mainPage: MediaPageEntity? = null
    var subpropertySelection = realmListOf<SubpropertySelectionEntity>()

    var loginInfo: PropertyLoginInfoRealmEntity? = null

    @Ignore
    val loginProvider: LoginProviders
        get() = loginInfo?.loginProvider ?: LoginProviders.ORY

    var permissionStates = realmDictionaryOf<PermissionStatesEntity?>()

    @field:Ignore
    override var resolvedPermissions: VolatilePermissionSettings? = null
    override var rawPermissions: PermissionSettingsEntity? = null
    override val permissionChildren: List<EntityWithPermissions>
        get() = listOfNotNull(mainPage)

    // Unique permissions that don't depend on parent hierarchy and can be resolved directly.
    // When user is not authorized to view the property, we redirect to another page, which is
    // technically still in the same property, but we show it anyway.
    var propertyPermissions: PermissionSettingsEntity? = null

    // Permissions settings that apply for search results.
    var searchPermissions: PermissionSettingsEntity? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaPropertyEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (headerLogoUrl != other.headerLogoUrl) return false
        if (image != other.image) return false
        if (bgImageUrl != other.bgImageUrl) return false
        if (mainPage != other.mainPage) return false
        if (subpropertySelection != other.subpropertySelection) return false
        if (loginInfo != other.loginInfo) return false
        if (permissionStates != other.permissionStates) return false
        if (resolvedPermissions != other.resolvedPermissions) return false
        if (rawPermissions != other.rawPermissions) return false
        if (propertyPermissions != other.propertyPermissions) return false
        if (searchPermissions != other.searchPermissions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (headerLogoUrl?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (bgImageUrl?.hashCode() ?: 0)
        result = 31 * result + (mainPage?.hashCode() ?: 0)
        result = 31 * result + subpropertySelection.hashCode()
        result = 31 * result + (loginInfo?.hashCode() ?: 0)
        result = 31 * result + permissionStates.hashCode()
        result = 31 * result + (resolvedPermissions?.hashCode() ?: 0)
        result = 31 * result + (rawPermissions?.hashCode() ?: 0)
        result = 31 * result + (propertyPermissions?.hashCode() ?: 0)
        result = 31 * result + (searchPermissions?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "MediaPropertyEntity(id='$id', name='$name', headerLogoUrl=$headerLogoUrl, image=$image, bgImageUrl=$bgImageUrl, mainPage=$mainPage, subpropertySelection=$subpropertySelection, loginInfo=$loginInfo, permissionStates=$permissionStates, resolvedPermissions=$resolvedPermissions, rawPermissions=$rawPermissions, propertyPermissions=$propertyPermissions, searchPermissions=$searchPermissions)"
    }

    // Index can't be saved as part of the PropertyEntity object because it will get overridden
    // when fetching a single property from the API.
    class PropertyOrderEntity : RealmObject {
        @PrimaryKey
        var propertyId: String = ""
        var index: Int = Int.MAX_VALUE

        override fun toString(): String {
            return "PropertyOrderEntity(propertyId='$propertyId', index=$index)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PropertyOrderEntity

            if (propertyId != other.propertyId) return false
            if (index != other.index) return false

            return true
        }

        override fun hashCode(): Int {
            var result = propertyId.hashCode()
            result = 31 * result + index
            return result
        }
    }

    class SubpropertySelectionEntity : EmbeddedRealmObject {
        var id: String = ""
        var title: String? = null
        var icon: FabricUrlEntity? = null
        var tile: FabricUrlEntity? = null

        override fun toString(): String {
            return "SubpropertySelectionEntity(id='$id', title=$title, icon=$icon, tile=$tile)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SubpropertySelectionEntity

            if (id != other.id) return false
            if (title != other.title) return false
            if (icon != other.icon) return false
            if (tile != other.tile) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (title?.hashCode() ?: 0)
            result = 31 * result + (icon?.hashCode() ?: 0)
            result = 31 * result + (tile?.hashCode() ?: 0)
            return result
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntities(): Set<KClass<out TypedRealmObject>> =
            setOf(MediaPropertyEntity::class, PropertyOrderEntity::class, SubpropertySelectionEntity::class)
    }
}
