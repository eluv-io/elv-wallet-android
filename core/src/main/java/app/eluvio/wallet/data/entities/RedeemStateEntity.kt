package app.eluvio.wallet.data.entities

import app.eluvio.wallet.util.realm.RealmEnum
import app.eluvio.wallet.util.realm.realmEnum
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import kotlin.reflect.KClass

class RedeemStateEntity : EmbeddedRealmObject {
    var offerId: String = ""
    var active: Boolean = false
    var redeemer: String? = null
    var redeemed: RealmInstant? = null
    var transaction: String? = null
    private var statusStr: String = RedeemStatus.UNREDEEMED.value

    @Ignore
    var status: RedeemStatus by realmEnum(::statusStr)

    enum class RedeemStatus(override val value: String) : RealmEnum {
        UNREDEEMED("UNREDEEMED"),
        REDEEMING("REDEEMING"),
        REDEEMED_BY_CURRENT_USER("REDEEMED_BY_CURRENT_USER"),
        REDEEMED_BY_ANOTHER_USER("REDEEMED_BY_ANOTHER_USER"),
        REDEEM_FAILED("REDEEM_FAILED")
    }

    override fun toString(): String {
        return "RedeemStateEntity(offerId='$offerId', active=$active, redeemer=$redeemer, redeemed=$redeemed, transaction=$transaction, statusStr='$statusStr')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedeemStateEntity

        if (offerId != other.offerId) return false
        if (active != other.active) return false
        if (redeemer != other.redeemer) return false
        if (redeemed != other.redeemed) return false
        if (transaction != other.transaction) return false
        if (statusStr != other.statusStr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offerId.hashCode()
        result = 31 * result + active.hashCode()
        result = 31 * result + (redeemer?.hashCode() ?: 0)
        result = 31 * result + (redeemed?.hashCode() ?: 0)
        result = 31 * result + (transaction?.hashCode() ?: 0)
        result = 31 * result + statusStr.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = RedeemStateEntity::class
    }
}
