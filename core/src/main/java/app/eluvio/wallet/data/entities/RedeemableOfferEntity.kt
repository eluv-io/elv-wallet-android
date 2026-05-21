package app.eluvio.wallet.data.entities

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import kotlin.reflect.KClass

class RedeemableOfferEntity : EmbeddedRealmObject {
    var name: String = ""
    var description: String = ""
    var offerId: String = ""
    var imagePath: String? = null
    var posterImagePath: String? = null
    var availableAt: RealmInstant? = null
    var expiresAt: RealmInstant? = null

    // Relative paths to offer animations
    var animation: RealmDictionary<String> = realmDictionaryOf()
    var redeemAnimation: RealmDictionary<String> = realmDictionaryOf()

    /** Don't use this directly. Check [shouldHide] instead */
    var hide: Boolean = false
    var hideIfExpired: Boolean = false
    var hideIfUnreleased: Boolean = false

    @Ignore
    val availableNow: Boolean get() = !unreleased && !expired

    @Ignore
    val unreleased: Boolean get() = (availableAt ?: RealmInstant.MIN) > RealmInstant.now()

    @Ignore
    val expired: Boolean get() = (expiresAt ?: RealmInstant.MAX) < RealmInstant.now()

    /**
     * Whether or not the offer should be hidden from the UI.
     */
    @Ignore
    val shouldHide: Boolean
        get() = hide ||
                (hideIfExpired && expired) ||
                (hideIfUnreleased && unreleased)

    fun getFulfillmentState(redeemState: RedeemStateEntity): FulfillmentState {
        return when {
            redeemState.status == RedeemStateEntity.RedeemStatus.REDEEMED_BY_ANOTHER_USER -> FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER
            unreleased -> FulfillmentState.UNRELEASED
            expired -> FulfillmentState.EXPIRED
            else -> FulfillmentState.AVAILABLE
        }
    }

    enum class FulfillmentState {
        AVAILABLE,
        CLAIMED_BY_PREVIOUS_OWNER,
        EXPIRED,
        UNRELEASED,
    }

    override fun toString(): String {
        return "RedeemableOfferEntity(name='$name', description='$description', offerId='$offerId', imagePath=$imagePath, posterImagePath=$posterImagePath, availableAt=$availableAt, expiresAt=$expiresAt, animation=$animation, redeemAnimation=$redeemAnimation, hide=$hide, hideIfExpired=$hideIfExpired, hideIfUnreleased=$hideIfUnreleased)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedeemableOfferEntity

        if (name != other.name) return false
        if (description != other.description) return false
        if (offerId != other.offerId) return false
        if (imagePath != other.imagePath) return false
        if (posterImagePath != other.posterImagePath) return false
        if (availableAt != other.availableAt) return false
        if (expiresAt != other.expiresAt) return false
        if (animation != other.animation) return false
        if (redeemAnimation != other.redeemAnimation) return false
        if (hide != other.hide) return false
        if (hideIfExpired != other.hideIfExpired) return false
        if (hideIfUnreleased != other.hideIfUnreleased) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + offerId.hashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        result = 31 * result + (posterImagePath?.hashCode() ?: 0)
        result = 31 * result + (availableAt?.hashCode() ?: 0)
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        result = 31 * result + animation.hashCode()
        result = 31 * result + redeemAnimation.hashCode()
        result = 31 * result + hide.hashCode()
        result = 31 * result + hideIfExpired.hashCode()
        result = 31 * result + hideIfUnreleased.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = RedeemableOfferEntity::class
    }
}
