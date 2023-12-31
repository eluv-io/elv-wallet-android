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

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = RedeemStateEntity::class
    }
}
