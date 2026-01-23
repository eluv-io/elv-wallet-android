package app.eluvio.wallet.data.entities

import app.eluvio.wallet.data.FabricUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

/**
 * Represents a URL to fabric content.
 * Usually we don't want entities to fail equality checks when they have different base URLs, so
 * this class is preferred over simple string URLs, as it takes care of ignoring base URLs.
 * This also allows us to easily change the base URL across the entire Realm database when the
 * config gives us a new node URL.
 */
class FabricUrlEntity : EmbeddedRealmObject, FabricUrl {

    /**
     * Fully formed URL to fabric content (auth token not included).
     */
    override val url: String?
        get() {
            val baseUrl = baseUrl?.ifEmpty { null } ?: return null
            val path = path?.ifEmpty { null } ?: return null
            return "${baseUrl.removeSuffix("/")}/$path"
        }

    /** Don't read directly, use [url] instead. */
    private var path: String? = null
    private var baseUrl: String? = null

    /** ThumbHash placeholder for the image, base64 encoded */
    override var imageHash: String? = null
        private set

    fun set(baseUrl: String, path: String, imageHash: String? = null) {
        this.baseUrl = baseUrl
        this.path = path
        this.imageHash = imageHash
    }

    fun updateBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    override fun toString(): String {
        return "FabricUrlEntity(path=$path, baseUrl=$baseUrl, imageHash=$imageHash)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FabricUrlEntity

        // Ignore baseUrl when comparing.
        if (path != other.path) return false
        if (imageHash != other.imageHash) return false

        return true
    }

    override fun hashCode(): Int {
        // Ignore baseUrl when comparing.
        var result = path?.hashCode() ?: 0
        result = 31 * result + (imageHash?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = FabricUrlEntity::class
    }
}
