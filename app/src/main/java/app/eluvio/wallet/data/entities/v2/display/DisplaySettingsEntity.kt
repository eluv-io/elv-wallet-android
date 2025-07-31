package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.data.entities.FabricUrlEntity
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.util.realm.realmEnum
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import kotlin.reflect.KClass

class DisplaySettingsEntity : EmbeddedRealmObject, DisplaySettings {
    override var title: String? = null
    override var subtitle: String? = null
    override var headers = realmListOf<String>()
    override var description: String? = null

    override var forcedAspectRatio: Float? = null
    override var thumbnailLandscapeUrl: FabricUrlEntity? = null
    override var thumbnailPortraitUrl: FabricUrlEntity? = null
    override var thumbnailSquareUrl: FabricUrlEntity? = null

    override var displayLimit: Int? = null
    override var displayLimitType: String? = null

    @Ignore
    override var displayFormat: DisplayFormat by realmEnum(::_displayFormat)
    private var _displayFormat: String = DisplayFormat.UNKNOWN.value

    override var logoUrl: FabricUrlEntity? = null
    override var logoText: String? = null
    override var inlineBackgroundColor: String? = null
    override var inlineBackgroundImageUrl: FabricUrlEntity? = null

    override var heroBackgroundImageUrl: FabricUrlEntity? = null
    override var heroBackgroundVideoHash: String? = null

    override var hiddenOnTv: Boolean? = null

    override var fullBleed: Boolean? = null

    override fun toString(): String {
        return "DisplaySettingsEntity(title=$title, subtitle=$subtitle, headers=$headers, description=$description, forcedAspectRatio=$forcedAspectRatio, thumbnailLandscapeUrl=$thumbnailLandscapeUrl, thumbnailPortraitUrl=$thumbnailPortraitUrl, thumbnailSquareUrl=$thumbnailSquareUrl, displayLimit=$displayLimit, displayLimitType=$displayLimitType, _displayFormat='$_displayFormat', logoUrl=$logoUrl, logoText=$logoText, inlineBackgroundColor=$inlineBackgroundColor, inlineBackgroundImageUrl=$inlineBackgroundImageUrl, heroBackgroundImageUrl=$heroBackgroundImageUrl, heroBackgroundVideoHash=$heroBackgroundVideoHash, hiddenOnTv=$hiddenOnTv, fullBleed=$fullBleed)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DisplaySettingsEntity

        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (headers != other.headers) return false
        if (description != other.description) return false
        if (forcedAspectRatio != other.forcedAspectRatio) return false
        if (thumbnailLandscapeUrl != other.thumbnailLandscapeUrl) return false
        if (thumbnailPortraitUrl != other.thumbnailPortraitUrl) return false
        if (thumbnailSquareUrl != other.thumbnailSquareUrl) return false
        if (displayLimit != other.displayLimit) return false
        if (displayLimitType != other.displayLimitType) return false
        if (_displayFormat != other._displayFormat) return false
        if (logoUrl != other.logoUrl) return false
        if (logoText != other.logoText) return false
        if (inlineBackgroundColor != other.inlineBackgroundColor) return false
        if (inlineBackgroundImageUrl != other.inlineBackgroundImageUrl) return false
        if (heroBackgroundImageUrl != other.heroBackgroundImageUrl) return false
        if (heroBackgroundVideoHash != other.heroBackgroundVideoHash) return false
        if (hiddenOnTv != other.hiddenOnTv) return false
        if (fullBleed != other.fullBleed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (forcedAspectRatio?.hashCode() ?: 0)
        result = 31 * result + (thumbnailLandscapeUrl?.hashCode() ?: 0)
        result = 31 * result + (thumbnailPortraitUrl?.hashCode() ?: 0)
        result = 31 * result + (thumbnailSquareUrl?.hashCode() ?: 0)
        result = 31 * result + (displayLimit ?: 0)
        result = 31 * result + (displayLimitType?.hashCode() ?: 0)
        result = 31 * result + _displayFormat.hashCode()
        result = 31 * result + (logoUrl?.hashCode() ?: 0)
        result = 31 * result + (logoText?.hashCode() ?: 0)
        result = 31 * result + (inlineBackgroundColor?.hashCode() ?: 0)
        result = 31 * result + (inlineBackgroundImageUrl?.hashCode() ?: 0)
        result = 31 * result + (heroBackgroundImageUrl?.hashCode() ?: 0)
        result = 31 * result + (heroBackgroundVideoHash?.hashCode() ?: 0)
        result = 31 * result + (hiddenOnTv?.hashCode() ?: 0)
        result = 31 * result + (fullBleed?.hashCode() ?: 0)
        return result
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = DisplaySettingsEntity::class
    }
}
