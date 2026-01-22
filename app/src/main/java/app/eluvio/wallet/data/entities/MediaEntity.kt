package app.eluvio.wallet.data.entities

import androidx.compose.runtime.Immutable
import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.FabricUrl
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.data.entities.v2.display.DisplaySettingsEntity
import app.eluvio.wallet.data.entities.v2.display.SimpleDisplaySettings
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.VolatilePermissionSettings
import app.eluvio.wallet.data.entities.v2.search.FilterAttributeEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

// Realm classes are mutable by nature, but we make sure to only use them immutably, so adding this
// annotation helps Compose treat them as immutable and avoid unnecessary recompositions.
@Immutable
/**
 * This entity can represent a v1 or v2 media object. This was probably a mistake and we should
 * have made separate classes, but it's too late now :)
 * Note that for historical reasons, MediaItems *are* [DisplaySettings], but for consistency with
 * the rest of the v2 data model, on the Entity level we translate it to a [DisplaySettingsEntity].
 */
class MediaEntity : RealmObject, EntityWithPermissions {
    @PrimaryKey
    var id: String = ""
    var name: String = ""

    // Full path. Can't be converted to [FabricUrl] because it's coming as a full path from the v1 API.
    var image: String = ""
    var posterImagePath: String? = null
    var mediaType: String = ""
    var imageAspectRatio: Float? = null

    @field:Ignore
    override var resolvedPermissions: VolatilePermissionSettings? = null
    override var rawPermissions: PermissionSettingsEntity? = null
    override val permissionChildren: List<EntityWithPermissions>
        get() = emptyList()

    // Relative path to file
    var mediaFile: String = ""

    // A hash to the playable file.
    // This hash might be outdated, but the playout API will take care of that.
    var playableHash: String? = null

    // Relative paths to offerings. Only use if [playableHash] is null.
    var mediaLinks: RealmDictionary<String> = realmDictionaryOf()

    var tvBackgroundImage: String = ""

    var gallery: RealmList<GalleryItemEntity> = realmListOf()

    // Only applies to Media Lists and Media Collections
    var mediaItemsIds: RealmList<String> = realmListOf()

    var lockedState: LockedStateEntity? = null

    // In the mwv2 data model, all video is of type "Video" and this boolean tells live vs on-demand apart.
    var liveVideoInfo: LiveVideoInfoEntity? = null

    // Search API
    var attributes: RealmList<FilterAttributeEntity> = realmListOf()
    var tags: RealmList<String> = realmListOf()

    var displaySettings: DisplaySettingsEntity? = null

    var additionalViews: RealmList<AdditionalViewEntity> = realmListOf()

    // If display settings aren't directly available, construct them from legacy fields.
    fun requireDisplaySettings(): DisplaySettings {
        return displaySettings ?: defaultDisplaySettings()
    }

    fun imageOrLockedImage(): String = with(requireLockedState()) {
        lockedImage?.takeIf { locked } ?: image
    }

    fun nameOrLockedName(): String = with(requireLockedState()) {
        lockedName?.takeIf { locked } ?: name
    }

    /**
     * Returns the aspect ratio of the image, or the locked aspect ratio if locked.
     * If neither are set, returns [AspectRatio.SQUARE].
     */
    fun aspectRatio(): Float {
        val lockedState = requireLockedState()
        return lockedState.imageAspectRatio.takeIf { lockedState.locked }
            ?: imageAspectRatio
            ?: AspectRatio.SQUARE
    }

    fun requireLockedState(): LockedStateEntity {
        return lockedState ?: LockedStateEntity()
    }

    fun shouldBeHidden(): Boolean {
        return with(requireLockedState()) { locked && hideWhenLocked }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (!image.equalsIgnoreHost(other.image)) return false
        if (posterImagePath != other.posterImagePath) return false
        if (mediaType != other.mediaType) return false
        if (imageAspectRatio != other.imageAspectRatio) return false
        if (rawPermissions != other.rawPermissions) return false
        if (resolvedPermissions != other.resolvedPermissions) return false
        if (mediaFile != other.mediaFile) return false
        if (playableHash != other.playableHash) return false
        if (mediaLinks != other.mediaLinks) return false
        if (tvBackgroundImage != other.tvBackgroundImage) return false
        if (gallery != other.gallery) return false
        if (mediaItemsIds != other.mediaItemsIds) return false
        if (lockedState != other.lockedState) return false
        if (liveVideoInfo != other.liveVideoInfo) return false
        if (attributes != other.attributes) return false
        if (tags != other.tags) return false
        if (displaySettings != other.displaySettings) return false
        if (additionalViews != other.additionalViews) return false

        return true
    }

    private fun String?.equalsIgnoreHost(other: String?): Boolean {
        val host = "contentfabric.io"
        return this?.substringAfter(host) == other?.substringAfter(host)
    }

    private fun String?.hashCodeIgnoreHost(): Int {
        val host = "contentfabric.io"
        return this?.substringAfter(host)?.hashCode() ?: 0
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + image.hashCodeIgnoreHost()
        result = 31 * result + (posterImagePath?.hashCode() ?: 0)
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + (imageAspectRatio?.hashCode() ?: 0)
        result = 31 * result + (rawPermissions?.hashCode() ?: 0)
        result = 31 * result + (resolvedPermissions?.hashCode() ?: 0)
        result = 31 * result + mediaFile.hashCode()
        result = 31 * result + (playableHash?.hashCode() ?: 0)
        result = 31 * result + mediaLinks.hashCode()
        result = 31 * result + tvBackgroundImage.hashCode()
        result = 31 * result + gallery.hashCode()
        result = 31 * result + mediaItemsIds.hashCode()
        result = 31 * result + (lockedState?.hashCode() ?: 0)
        result = 31 * result + (liveVideoInfo?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (displaySettings?.hashCode() ?: 0)
        result = 31 * result + additionalViews.hashCode()
        return result
    }

    override fun toString(): String {
        return "MediaEntity(id='$id', name='$name', image='$image', posterImagePath=$posterImagePath, mediaType='$mediaType', imageAspectRatio=$imageAspectRatio, resolvedPermissions=$resolvedPermissions, rawPermissions=$rawPermissions, mediaFile='$mediaFile', playableHash=$playableHash, mediaLinks=$mediaLinks, tvBackgroundImage='$tvBackgroundImage', gallery=$gallery, mediaItemsIds=$mediaItemsIds, lockedState=$lockedState, liveVideoInfo=$liveVideoInfo, attributes=$attributes, tags=$tags, displaySettings=$displaySettings)"
    }

    companion object {
        const val MEDIA_TYPE_AUDIO = "Audio"
        const val MEDIA_TYPE_EBOOK = "Ebook"
        const val MEDIA_TYPE_GALLERY = "Gallery"
        const val MEDIA_TYPE_HTML = "HTML"
        const val MEDIA_TYPE_IMAGE = "Image"
        const val MEDIA_TYPE_LIVE = "Live"
        const val MEDIA_TYPE_VIDEO = "Video"
        const val MEDIA_TYPE_LIVE_VIDEO = "Live Video"
    }

    class LockedStateEntity : EmbeddedRealmObject {
        var locked: Boolean = false
        var hideWhenLocked: Boolean = false

        // full path to image
        var lockedImage: String? = null
        var lockedName: String? = null

        var imageAspectRatio: Float? = null
        var subtitle: String? = null

        override fun toString(): String {
            return "LockedStateEntity(locked=$locked, hideWhenLocked=$hideWhenLocked, lockedImage=$lockedImage, lockedName=$lockedName, imageAspectRatio=$imageAspectRatio, subtitle=$subtitle)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LockedStateEntity

            if (locked != other.locked) return false
            if (hideWhenLocked != other.hideWhenLocked) return false
            if (lockedImage != other.lockedImage) return false
            if (lockedName != other.lockedName) return false
            if (imageAspectRatio != other.imageAspectRatio) return false
            if (subtitle != other.subtitle) return false

            return true
        }

        override fun hashCode(): Int {
            var result = locked.hashCode()
            result = 31 * result + hideWhenLocked.hashCode()
            result = 31 * result + (lockedImage?.hashCode() ?: 0)
            result = 31 * result + (lockedName?.hashCode() ?: 0)
            result = 31 * result + (imageAspectRatio?.hashCode() ?: 0)
            result = 31 * result + (subtitle?.hashCode() ?: 0)
            return result
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntity(): Set<KClass<out TypedRealmObject>> =
            setOf(MediaEntity::class, LockedStateEntity::class, AdditionalViewEntity::class)
    }
}

class AdditionalViewEntity : EmbeddedRealmObject {
    var label: String = ""
    var imageUrl: FabricUrlEntity? = null
    var playableHash: String? = null
    var mediaLinks: RealmDictionary<String> = realmDictionaryOf()

    override fun toString(): String {
        return "AdditionalViewEntity(label='$label', imageUrl=$imageUrl, playableHash=$playableHash, mediaLinks=$mediaLinks)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdditionalViewEntity

        if (label != other.label) return false
        if (imageUrl != other.imageUrl) return false
        if (playableHash != other.playableHash) return false
        if (mediaLinks != other.mediaLinks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + (playableHash?.hashCode() ?: 0)
        result = 31 * result + mediaLinks.hashCode()
        return result
    }
}

private fun MediaEntity.defaultDisplaySettings(): DisplaySettings {
    val base = SimpleDisplaySettings(title = name)
    // Not using FabricUrlEntity because [image] can be a fully formed URL and we
    // don't want to arbitrarily break it up to base/path.
    val imageUrl = object : FabricUrl {
        override val url: String = imageOrLockedImage()
    }
    return when (aspectRatio()) {
        AspectRatio.SQUARE -> base.copy(thumbnailSquareUrl = imageUrl)
        AspectRatio.WIDE -> base.copy(thumbnailLandscapeUrl = imageUrl)
        AspectRatio.POSTER -> base.copy(thumbnailPortraitUrl = imageUrl)
        else -> base
    }
}
