package app.eluvio.wallet.data.entities.v2.permissions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

/**
 * For every PermissionItem reachable from the Property, holds whether or not the user currently
 * has access to it.
 * PermissionsItems are children of PermissionSets defined in the Property, its sub-properties, and
 * any linked Media Catalogs.
 */
class PermissionStatesEntity : RealmObject {
    @PrimaryKey
    var id: String = ""
    var authorized: Boolean = false

    override fun toString(): String {
        return "PermissionStatesEntity(id='$id', authorized=$authorized)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionStatesEntity

        if (id != other.id) return false
        if (authorized != other.authorized) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + authorized.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = PermissionStatesEntity::class
    }
}
