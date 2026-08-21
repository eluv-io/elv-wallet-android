package app.eluvio.wallet.data.entities.v2

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

/**
 * A single row of Properties on the Discover page.
 * Rows have no server-side id, so they are keyed by their position in the response.
 */
class DiscoverRowEntity : RealmObject {
    @PrimaryKey
    var index: Int = 0
    var title: String = ""

    /** Featured rows get a bigger, hero-style treatment instead of a plain titled row. */
    var featured: Boolean = false
    var propertyIds = realmListOf<String>()

    override fun toString(): String {
        return "DiscoverRowEntity(index=$index, title='$title', featured=$featured, propertyIds=$propertyIds)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscoverRowEntity

        if (index != other.index) return false
        if (title != other.title) return false
        if (featured != other.featured) return false
        if (propertyIds != other.propertyIds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + title.hashCode()
        result = 31 * result + featured.hashCode()
        result = 31 * result + propertyIds.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntities(): Set<KClass<out TypedRealmObject>> = setOf(DiscoverRowEntity::class)
    }
}
