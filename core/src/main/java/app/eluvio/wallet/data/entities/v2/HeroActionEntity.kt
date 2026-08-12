package app.eluvio.wallet.data.entities.v2

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

/**
 * A CTA button defined on a hero item.
 * Colors are stored as the raw hex strings the server sent us.
 */
class HeroActionEntity : EmbeddedRealmObject {
    var id: String = ""

    /** Always one of [supportedBehaviors] - unsupported actions aren't stored at all. */
    var behavior: String? = null
    var mediaId: String? = null
    var pageId: String? = null
    var url: String? = null

    var text: String? = null

    var backgroundColor: String? = null
    var textColor: String? = null
    var borderColor: String? = null
    var borderRadius: Int? = null

    override fun toString(): String {
        return "HeroActionEntity(id='$id', behavior=$behavior, mediaId=$mediaId, pageId=$pageId, url=$url, text=$text, backgroundColor=$backgroundColor, textColor=$textColor, borderColor=$borderColor, borderRadius=$borderRadius)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HeroActionEntity

        if (id != other.id) return false
        if (behavior != other.behavior) return false
        if (mediaId != other.mediaId) return false
        if (pageId != other.pageId) return false
        if (url != other.url) return false
        if (text != other.text) return false
        if (backgroundColor != other.backgroundColor) return false
        if (textColor != other.textColor) return false
        if (borderColor != other.borderColor) return false
        if (borderRadius != other.borderRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (behavior?.hashCode() ?: 0)
        result = 31 * result + (mediaId?.hashCode() ?: 0)
        result = 31 * result + (pageId?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (backgroundColor?.hashCode() ?: 0)
        result = 31 * result + (textColor?.hashCode() ?: 0)
        result = 31 * result + (borderColor?.hashCode() ?: 0)
        result = 31 * result + (borderRadius ?: 0)
        return result
    }

    companion object {
        const val BEHAVIOR_MEDIA_LINK = "media_link"
        const val BEHAVIOR_PAGE_LINK = "page_link"
        const val BEHAVIOR_EXTERNAL_LINK = "link"

        val supportedBehaviors =
            setOf(BEHAVIOR_MEDIA_LINK, BEHAVIOR_PAGE_LINK, BEHAVIOR_EXTERNAL_LINK)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = HeroActionEntity::class
    }
}
