package app.eluvio.wallet.data.entities

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class FulfillmentDataEntity : RealmObject {
    @PrimaryKey
    var transactionHash: String = ""
    var message: String? = null
    var url: String? = null
    var code: String? = null

    override fun toString(): String {
        return "FulfillmentDataEntity(transactionHash='$transactionHash', message=$message, url=$url, code=$code)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FulfillmentDataEntity

        if (transactionHash != other.transactionHash) return false
        if (message != other.message) return false
        if (url != other.url) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transactionHash.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (code?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = FulfillmentDataEntity::class
    }
}
