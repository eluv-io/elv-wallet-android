package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.display.DisplaySettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class SectionItemEntity : RealmObject, EntityWithPermissions {
    @PrimaryKey
    var id: String = ""

    var mediaType: String? = null

    /** Not every item is of type Media, so this field is optional. */
    var media: MediaEntity? = null

    /** When exists, this item is a link to another page/property */
    var linkData: LinkData? = null

    var isPurchaseItem = false

    // This field is defined only if the SectionItem is inside a Banner section.
    var bannerImageUrl: FabricUrlEntity? = null

    var useMediaDisplaySettings: Boolean = true
    var displaySettings: DisplaySettingsEntity? = null

    @field:Ignore
    override var resolvedPermissions: VolatilePermissionSettings? = null
    override var rawPermissions: PermissionSettingsEntity? = null
    override val permissionChildren: List<EntityWithPermissions>
        get() = listOfNotNull(media)

    override fun toString(): String {
        return "SectionItemEntity(id='$id', mediaType=$mediaType, media=$media, linkData=$linkData, isPurchaseItem=$isPurchaseItem, bannerImageUrl=$bannerImageUrl, useMediaDisplaySettings=$useMediaDisplaySettings, displaySettings=$displaySettings, resolvedPermissions=$resolvedPermissions, rawPermissions=$rawPermissions)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SectionItemEntity

        if (id != other.id) return false
        if (mediaType != other.mediaType) return false
        if (media != other.media) return false
        if (linkData != other.linkData) return false
        if (isPurchaseItem != other.isPurchaseItem) return false
        if (bannerImageUrl != other.bannerImageUrl) return false
        if (useMediaDisplaySettings != other.useMediaDisplaySettings) return false
        if (displaySettings != other.displaySettings) return false
        if (rawPermissions != other.rawPermissions) return false
        if (resolvedPermissions != other.resolvedPermissions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (mediaType?.hashCode() ?: 0)
        result = 31 * result + (media?.hashCode() ?: 0)
        result = 31 * result + (linkData?.hashCode() ?: 0)
        result = 31 * result + isPurchaseItem.hashCode()
        result = 31 * result + (bannerImageUrl?.hashCode() ?: 0)
        result = 31 * result + useMediaDisplaySettings.hashCode()
        result = 31 * result + (displaySettings?.hashCode() ?: 0)
        result = 31 * result + (rawPermissions?.hashCode() ?: 0)
        result = 31 * result + (resolvedPermissions?.hashCode() ?: 0)
        return result
    }

    /** This section item links to a property/page. */
    class LinkData : EmbeddedRealmObject {
        /**
         * Present when the link is to a property, or a specific page within another property.
         */
        var linkPropertyId: String? = null

        /**
         * Present when the link is to a specific page within the current property, or a specific page within another property.
         */
        var linkPageId: String? = null

        /**
         * Present when the link is to an external URL, outside MW.
         */
        var externalLink: String? = null

        override fun toString(): String {
            return "LinkData(linkPropertyId=$linkPropertyId, linkPageId=$linkPageId, externalLink=$externalLink)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LinkData

            if (linkPropertyId != other.linkPropertyId) return false
            if (linkPageId != other.linkPageId) return false
            if (externalLink != other.externalLink) return false

            return true
        }

        override fun hashCode(): Int {
            var result = linkPropertyId?.hashCode() ?: 0
            result = 31 * result + (linkPageId?.hashCode() ?: 0)
            result = 31 * result + (externalLink?.hashCode() ?: 0)
            return result
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntity(): Set<KClass<out TypedRealmObject>> =
            setOf(SectionItemEntity::class, LinkData::class)
    }
}
