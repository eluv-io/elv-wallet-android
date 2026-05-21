package app.eluvio.wallet.data.entities

import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlin.reflect.KClass

class NftEntity : RealmObject {
    /**
     * @see [NftId]
     */
    @PrimaryKey
    var id: String = ""

    var nftTemplate: NftTemplateEntity? = null

    // This is actually the Template creation date, so multiple tokens from the same template will
    // still have unstable sorting. AFAIK there's no way to get the actual token creation date currently.
    var createdAt: Long = 0
    var tokenId: String = ""
    var versionHash: String? = null
    var imageUrl: String = ""
    var displayName: String = ""
    var redeemableOffers: RealmList<RedeemableOfferEntity> = realmListOf()

    // Info that can be null until fetched separately from nft/info/{contractAddress}/{tokenId}
    var redeemStates: RealmList<RedeemStateEntity> = realmListOf()

    @Ignore
    val contractAddress: String get() = nftTemplate?.contractAddress ?: ""

    @Ignore
    val description: String get() = nftTemplate?.description ?: ""

    @Ignore
    val descriptionRichText: String? get() = nftTemplate?.descriptionRichText

    @Ignore
    val featuredMedia: RealmList<MediaEntity> get() = nftTemplate?.featuredMedia ?: realmListOf()

    @Ignore
    val mediaSections: RealmList<MediaSectionEntity>
        get() = nftTemplate?.mediaSections.toRealmListOrEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NftEntity

        if (id != other.id) return false
        if (nftTemplate != other.nftTemplate) return false
        if (createdAt != other.createdAt) return false
        if (tokenId != other.tokenId) return false
        if (versionHash != other.versionHash) return false
        if (imageUrl != other.imageUrl) return false
        if (displayName != other.displayName) return false
        if (redeemableOffers != other.redeemableOffers) return false
        if (redeemStates != other.redeemStates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (nftTemplate?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + tokenId.hashCode()
        result = 31 * result + versionHash.hashCode()
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + redeemableOffers.hashCode()
        result = 31 * result + redeemStates.hashCode()
        return result
    }

    override fun toString(): String {
        return "NftEntity(id='$id', nftTemplate=$nftTemplate, createdAt=$createdAt, tokenId='$tokenId', versionHash='$versionHash', imageUrl='$imageUrl', displayName='$displayName', redeemableOffers=$redeemableOffers, redeemStates=$redeemStates)"
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = NftEntity::class
    }
}
