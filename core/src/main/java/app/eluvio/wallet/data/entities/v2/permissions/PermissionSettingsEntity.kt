package app.eluvio.wallet.data.entities.v2.permissions

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import kotlin.reflect.KClass

/**
 * Permissions are used to determine if a user has access to a specific object.
 * This object can be a property, a page, a section, a sectionItem, a media item, etc.
 */
class PermissionSettingsEntity : PermissionSettings, EmbeddedRealmObject {

    private var _permissionItemIds = realmListOf<String>()
    override var permissionItemIds: List<String>
        get() = _permissionItemIds
        set(value) {
            _permissionItemIds = value.toRealmList()
        }

    override var behavior: String? = null
    override var alternatePageId: String? = null

    // We just pass it as-is when launching purchase options
    override var secondaryMarketPurchaseOption: String? = null

    // Only applies to resolved permissions
    @field:Ignore
    override var authorized: Boolean? = null

    override fun toString(): String {
        return "PermissionSettingsEntity(_permissionItemIds=$_permissionItemIds, behavior=$behavior, alternatePageId=$alternatePageId, secondaryMarketPurchaseOption=$secondaryMarketPurchaseOption, authorized=$authorized)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionSettingsEntity

        if (_permissionItemIds != other._permissionItemIds) return false
        if (behavior != other.behavior) return false
        if (alternatePageId != other.alternatePageId) return false
        if (secondaryMarketPurchaseOption != other.secondaryMarketPurchaseOption) return false
        if (authorized != other.authorized) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _permissionItemIds.hashCode()
        result = 31 * result + (behavior?.hashCode() ?: 0)
        result = 31 * result + (alternatePageId?.hashCode() ?: 0)
        result = 31 * result + (secondaryMarketPurchaseOption?.hashCode() ?: 0)
        result = 31 * result + (authorized?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = PermissionSettingsEntity::class
    }
}
