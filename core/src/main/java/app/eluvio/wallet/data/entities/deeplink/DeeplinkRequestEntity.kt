package app.eluvio.wallet.data.entities.deeplink

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class DeeplinkRequestEntity : RealmObject {
    @PrimaryKey
    var id = "singleton"
    var action: String? = null
    var marketplace: String? = null
    var contract: String? = null
    var sku: String? = null
    var jwt: String? = null
    var entitlement: String? = null
    var backLink: String? = null

    override fun toString(): String {
        return "DeeplinkRequestEntity(id='$id', action=$action, marketplace=$marketplace, contract=$contract, sku=$sku, jwt=$jwt, entitlement=$entitlement, backLink=$backLink)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeeplinkRequestEntity

        if (id != other.id) return false
        if (action != other.action) return false
        if (marketplace != other.marketplace) return false
        if (contract != other.contract) return false
        if (sku != other.sku) return false
        if (jwt != other.jwt) return false
        if (entitlement != other.entitlement) return false
        if (backLink != other.backLink) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + (marketplace?.hashCode() ?: 0)
        result = 31 * result + (contract?.hashCode() ?: 0)
        result = 31 * result + (sku?.hashCode() ?: 0)
        result = 31 * result + (jwt?.hashCode() ?: 0)
        result = 31 * result + (entitlement?.hashCode() ?: 0)
        result = 31 * result + (backLink?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = DeeplinkRequestEntity::class
    }
}