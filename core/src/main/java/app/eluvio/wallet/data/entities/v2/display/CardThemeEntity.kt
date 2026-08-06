package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.util.realm.RealmEnum
import app.eluvio.wallet.util.realm.realmEnum
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import kotlin.reflect.KClass

/**
 * Defines the visuals of a card.
 * Themes are defined once per Property, and referenced by ID at any level of the hierarchy
 * (Property/Page/Section). See [app.eluvio.wallet.data.entities.v2.MediaPropertyEntity.resolveCardTheme].
 */
class CardThemeEntity : EmbeddedRealmObject {
    var id: String = ""

    @Ignore
    var borderRadius: CardBorderRadius by realmEnum(::_borderRadius)
    private var _borderRadius: String = CardBorderRadius.NONE.value

    /** Border thickness in dp. Zero means the card has no border of its own. */
    var borderWidth: Int = 0

    /** Renders square cards as circles. Cards with any other aspect ratio are unaffected. */
    var circularize: Boolean = false

    /** Hex color of the border while the card is focused. */
    var activeBorderColor: String? = null

    /** Hex color of the border while the card is not focused. */
    var inactiveBorderColor: String? = null

    override fun toString(): String {
        return "CardThemeEntity(id='$id', _borderRadius='$_borderRadius', borderWidth=$borderWidth, circularize=$circularize, activeBorderColor=$activeBorderColor, inactiveBorderColor=$inactiveBorderColor)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardThemeEntity

        if (id != other.id) return false
        if (_borderRadius != other._borderRadius) return false
        if (borderWidth != other.borderWidth) return false
        if (circularize != other.circularize) return false
        if (activeBorderColor != other.activeBorderColor) return false
        if (inactiveBorderColor != other.inactiveBorderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + _borderRadius.hashCode()
        result = 31 * result + borderWidth
        result = 31 * result + circularize.hashCode()
        result = 31 * result + (activeBorderColor?.hashCode() ?: 0)
        result = 31 * result + (inactiveBorderColor?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = CardThemeEntity::class
    }
}

enum class CardBorderRadius(override val value: String) : RealmEnum {
    NONE("none"),
    SUBTLE("subtle"),
    CURVED("curved"),
    ;

    companion object {
        fun from(value: String?): CardBorderRadius {
            return entries.firstOrNull { it.value == value } ?: NONE
        }
    }
}
