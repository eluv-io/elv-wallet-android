package app.eluvio.wallet.data.entities

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class NftEntity : RealmObject {
    @PrimaryKey
    var _id: String = ""

    // This is actually the Template creation date, so multiple tokens from the same template will
    // still have unstable sorting. AFAIK there's no way to get the actual token creation date currently.
    var createdAt: Long = 0
    var contractAddress: String = ""
    var tokenId: String = ""
    var imageUrl: String = ""
    var displayName: String = ""
    var editionName: String = ""
    var description: String = ""
    var descriptionRichText: String? = null
    var featuredMedia: RealmList<MediaEntity> = realmListOf()
    var mediaSections: RealmList<MediaSectionEntity> = realmListOf()
    var redeemableOffers: RealmList<RedeemableOfferEntity> = realmListOf()

    // Info that can be null until fetched separately from nft/info/{contractAddress}/{tokenId}
    var redeemStates: RealmList<RedeemStateEntity> = realmListOf()
    var tenant: String? = null

    override fun toString(): String {
        return "NftEntity(_id='$_id', contractAddress='$contractAddress', tokenId='$tokenId', imageUrl='$imageUrl', displayName='$displayName', editionName='$editionName', description='$description', featuredMedia=$featuredMedia, mediaSections=$mediaSections, redeemableOffers=$redeemableOffers, redeemStates=$redeemStates, tenant=$tenant)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NftEntity

        if (_id != other._id) return false
        if (createdAt != other.createdAt) return false
        if (contractAddress != other.contractAddress) return false
        if (tokenId != other.tokenId) return false
        if (imageUrl != other.imageUrl) return false
        if (displayName != other.displayName) return false
        if (editionName != other.editionName) return false
        if (description != other.description) return false
        if (descriptionRichText != other.descriptionRichText) return false
        if (featuredMedia != other.featuredMedia) return false
        if (mediaSections != other.mediaSections) return false
        if (redeemableOffers != other.redeemableOffers) return false
        if (redeemStates != other.redeemStates) return false
        if (tenant != other.tenant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _id.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + contractAddress.hashCode()
        result = 31 * result + tokenId.hashCode()
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + editionName.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (descriptionRichText?.hashCode() ?: 0)
        result = 31 * result + featuredMedia.hashCode()
        result = 31 * result + mediaSections.hashCode()
        result = 31 * result + redeemableOffers.hashCode()
        result = 31 * result + redeemStates.hashCode()
        result = 31 * result + (tenant?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = NftEntity::class
    }
}
