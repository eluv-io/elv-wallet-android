package app.eluvio.wallet.data.entities

import app.eluvio.wallet.data.AspectRatio
import app.eluvio.wallet.data.entities.v2.SearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.permissions.EntityWithPermissions
import app.eluvio.wallet.data.entities.v2.permissions.PermissionsEntity
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

class MediaEntity : RealmObject, EntityWithPermissions {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var image: String = ""
    var posterImagePath: String? = null
    var mediaType: String = ""
    var imageAspectRatio: Float? = null

    @Ignore
    override var resolvedPermissions: PermissionsEntity? = null
    override var rawPermissions: PermissionsEntity? = null
    override val permissionChildren: List<EntityWithPermissions>
        get() = emptyList()

    // Relative path to file
    var mediaFile: String = ""

    // Relative paths to offerings
    var mediaLinks: RealmDictionary<String> = realmDictionaryOf()

    var tvBackgroundImage: String = ""

    var gallery: RealmList<GalleryItemEntity> = realmListOf()

    // Only applies to Media Lists and Media Collections
    var mediaItemsIds: RealmList<String> = realmListOf()

    var lockedState: LockedStateEntity? = null

    // In the mwv2 data model, all video is of type "Video" and this boolean tells live vs on-demand apart.
    var liveVideoInfo: LiveVideoInfoEntity? = null

    // Search API
    var attributes: RealmList<SearchFiltersEntity.Attribute> = realmListOf()
    var tags: RealmList<String> = realmListOf()

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
        if (resolvedPermissions != other.resolvedPermissions) return false
        if (rawPermissions != other.rawPermissions) return false
        if (mediaFile != other.mediaFile) return false
        if (mediaLinks != other.mediaLinks) return false
        if (tvBackgroundImage != other.tvBackgroundImage) return false
        if (gallery != other.gallery) return false
        if (mediaItemsIds != other.mediaItemsIds) return false
        if (lockedState != other.lockedState) return false
        if (liveVideoInfo != other.liveVideoInfo) return false
        if (attributes != other.attributes) return false
        if (tags != other.tags) return false

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
        result = 31 * result + (resolvedPermissions?.hashCode() ?: 0)
        result = 31 * result + (rawPermissions?.hashCode() ?: 0)
        result = 31 * result + mediaFile.hashCode()
        result = 31 * result + mediaLinks.hashCode()
        result = 31 * result + tvBackgroundImage.hashCode()
        result = 31 * result + gallery.hashCode()
        result = 31 * result + mediaItemsIds.hashCode()
        result = 31 * result + (lockedState?.hashCode() ?: 0)
        result = 31 * result + (liveVideoInfo?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }

    override fun toString(): String {
        return "MediaEntity(id='$id', name='$name', image='$image', posterImagePath=$posterImagePath, mediaType='$mediaType', imageAspectRatio=$imageAspectRatio, resolvedPermissions=$resolvedPermissions, rawPermissions=$rawPermissions, mediaFile='$mediaFile', mediaLinks=$mediaLinks, tvBackgroundImage='$tvBackgroundImage', gallery=$gallery, mediaItemsIds=$mediaItemsIds, lockedState=$lockedState, liveVideoInfo=$liveVideoInfo, attributes=$attributes, tags=$tags)"
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
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntity(): Set<KClass<out TypedRealmObject>> =
            setOf(MediaEntity::class, LockedStateEntity::class)
    }
}
