package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.util.realm.RealmEnum
import app.eluvio.wallet.util.realm.realmEnum
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
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

    /** Visuals that differ between the focused ("active") and unfocused states. */
    var active: CardThemeStateEntity? = null
    var inactive: CardThemeStateEntity? = null

    fun state(focused: Boolean): CardThemeStateEntity? = if (focused) active else inactive

    override fun toString(): String {
        return "CardThemeEntity(id='$id', _borderRadius='$_borderRadius', borderWidth=$borderWidth, circularize=$circularize, active=$active, inactive=$inactive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardThemeEntity

        if (id != other.id) return false
        if (_borderRadius != other._borderRadius) return false
        if (borderWidth != other.borderWidth) return false
        if (circularize != other.circularize) return false
        if (active != other.active) return false
        if (inactive != other.inactive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + _borderRadius.hashCode()
        result = 31 * result + borderWidth
        result = 31 * result + circularize.hashCode()
        result = 31 * result + (active?.hashCode() ?: 0)
        result = 31 * result + (inactive?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @ElementsIntoSet
        fun provideEntities(): Set<KClass<out TypedRealmObject>> =
            setOf(CardThemeEntity::class, CardThemeStateEntity::class)
    }
}

/**
 * The half of a card theme that depends on whether the card is focused.
 * Colors are hex strings, as they come from the API.
 */
class CardThemeStateEntity : EmbeddedRealmObject {
    var borderColor: String? = null

    /** Solid fill, or the first stop when [gradient] is true. */
    var backgroundColor: String? = null

    /** Percent, 0-100. */
    var backgroundColorOpacity: Int = 100

    /** Second stop of the gradient. Only meaningful when [gradient] is true. */
    var backgroundColor2: String? = null
    var backgroundColor2Opacity: Int = 100

    /** Degrees, clockwise from "up", like a CSS gradient. */
    var backgroundGradientAngle: Int = 0

    var gradient: Boolean = false

    override fun toString(): String {
        return "CardThemeStateEntity(borderColor=$borderColor, backgroundColor=$backgroundColor, backgroundColorOpacity=$backgroundColorOpacity, backgroundColor2=$backgroundColor2, backgroundColor2Opacity=$backgroundColor2Opacity, backgroundGradientAngle=$backgroundGradientAngle, gradient=$gradient)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardThemeStateEntity

        if (borderColor != other.borderColor) return false
        if (backgroundColor != other.backgroundColor) return false
        if (backgroundColorOpacity != other.backgroundColorOpacity) return false
        if (backgroundColor2 != other.backgroundColor2) return false
        if (backgroundColor2Opacity != other.backgroundColor2Opacity) return false
        if (backgroundGradientAngle != other.backgroundGradientAngle) return false
        if (gradient != other.gradient) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor?.hashCode() ?: 0
        result = 31 * result + (backgroundColor?.hashCode() ?: 0)
        result = 31 * result + backgroundColorOpacity
        result = 31 * result + (backgroundColor2?.hashCode() ?: 0)
        result = 31 * result + backgroundColor2Opacity
        result = 31 * result + backgroundGradientAngle
        result = 31 * result + gradient.hashCode()
        return result
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
