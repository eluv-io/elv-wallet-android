package app.eluvio.wallet.data.entities.v2

import app.eluvio.wallet.data.entities.FabricUrlEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

class PropertyLoginInfoRealmEntity : EmbeddedRealmObject {

    var backgroundImageUrl: FabricUrlEntity? = null
    var logoUrl: FabricUrlEntity? = null

    // Opaque identifier for the Property's login provider - only ever compared to another
    // Property's. Comes from the server's "provider_id" when it has one, otherwise we derive an
    // equivalent string ourselves. See MediaPropertyConverter.toLoginProvider.
    var loginProvider: String? = null

    var skipLogin: Boolean = false

    override fun toString(): String {
        return "PropertyLoginInfoRealmEntity(backgroundImageUrl=$backgroundImageUrl, logoUrl=$logoUrl, loginProvider=$loginProvider, loginProvider='$loginProvider', skipLogin=$skipLogin)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PropertyLoginInfoRealmEntity

        if (backgroundImageUrl != other.backgroundImageUrl) return false
        if (logoUrl != other.logoUrl) return false
        if (loginProvider != other.loginProvider) return false
        if (skipLogin != other.skipLogin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundImageUrl?.hashCode() ?: 0
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        result = 31 * result + loginProvider.hashCode()
        result = 31 * result + skipLogin.hashCode()
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = PropertyLoginInfoRealmEntity::class
    }
}
