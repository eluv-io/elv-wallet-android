package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class MediaPageEntity : RealmObject, EntityWithPermissions {
    companion object {
        fun uid(propertyId: String, pageId: String): String {
            return "$propertyId-$pageId"
        }
    }

    /**
     * This is a composite ID we create with the containing Property's ID,
     * to make pages IDs unique across all properties.
     */
    @PrimaryKey
    var uid: String = ""

    /**
     * The real ID of the page, as it comes from the API.
     * We need this for API calls that require pageId.
     */
    var id: String = ""

    var backgroundImageUrl: FabricUrlEntity? = null

    /** AKA Header logo or banner */
    var logoUrl: FabricUrlEntity? = null
    var title: String? = null
    var description: String? = ""

    /**
     * Only to be used if both [title] and [description] are missing.
     * Rich text never looks good on TV, so this is only a fallback.
     */
    var descriptionRichText: String? = null

    /**
     * Full list of Section IDs for this Media Page.
     * We might not have all of them in our local cache already, since it comes in pages from the API.
     */
    var sectionIds = realmListOf<String>()

    @field:Ignore
    override var resolvedPermissions: VolatilePermissionSettings? = null
    override var rawPermissions: PermissionSettingsEntity? = null
    override val permissionChildren: List<EntityWithPermissions>
        // Sections aren't directly connected to the object, so stop propagation here.
        get() = emptyList()

    // Unique permissions that don't depend on parent hierarchy and can be resolved directly.
    // These permissions say whether or not the user is allowed to view this page
    var pagePermissions: PermissionSettingsEntity? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaPageEntity

        if (uid != other.uid) return false
        if (id != other.id) return false
        if (backgroundImageUrl != other.backgroundImageUrl) return false
        if (logoUrl != other.logoUrl) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (descriptionRichText != other.descriptionRichText) return false
        if (sectionIds != other.sectionIds) return false
        if (rawPermissions != other.rawPermissions) return false
        if (resolvedPermissions != other.resolvedPermissions) return false
        if (pagePermissions != other.pagePermissions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (backgroundImageUrl?.hashCode() ?: 0)
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (descriptionRichText?.hashCode() ?: 0)
        result = 31 * result + sectionIds.hashCode()
        result = 31 * result + (rawPermissions?.hashCode() ?: 0)
        result = 31 * result + (resolvedPermissions?.hashCode() ?: 0)
        result = 31 * result + (pagePermissions?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "MediaPageEntity(id='$uid', realId='$id', backgroundImage=$backgroundImageUrl, logoUrl=$logoUrl, title=$title, description=$description, descriptionRichText=$descriptionRichText, sectionIds=$sectionIds, resolvedPermissions=$resolvedPermissions, rawPermissions=$rawPermissions, pagePermissions=$pagePermissions)"
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = MediaPageEntity::class
    }
}
