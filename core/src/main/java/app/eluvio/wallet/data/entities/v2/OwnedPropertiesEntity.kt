package app.eluvio.wallet.data.entities.v2

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

interface OwnedPropertiesEntity {
    /**
     * A dictionary of properties ID->Name that the current user has access to by owning a relevant NFT.
     */
    val properties: Map<String, String>
}

class OwnedPropertiesRealmEntity : RealmObject, OwnedPropertiesEntity {
    @PrimaryKey
    var id: String = "singleton"

    override var properties = realmDictionaryOf<String>()

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = OwnedPropertiesRealmEntity::class
    }

    override fun toString(): String {
        return "OwnedPropertiesRealmEntity(id='$id', properties=$properties)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OwnedPropertiesRealmEntity

        if (id != other.id) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}
