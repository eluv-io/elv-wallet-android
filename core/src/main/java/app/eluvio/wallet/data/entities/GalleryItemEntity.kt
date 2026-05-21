package app.eluvio.wallet.data.entities

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

class GalleryItemEntity : EmbeddedRealmObject {
    var name: String? = null
    var imagePath: String? = null
    var imageAspectRatio: Float? = null

    override fun toString(): String {
        return "GalleryItemEntity(name='$name', imagePath=$imagePath)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryItemEntity

        if (name != other.name) return false
        if (imagePath != other.imagePath) return false
        if (imageAspectRatio != other.imageAspectRatio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        result = 31 * result + (imageAspectRatio?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = GalleryItemEntity::class
    }
}
