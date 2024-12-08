package app.eluvio.wallet.data.entities

import android.content.Context
import android.text.format.DateFormat
import app.eluvio.wallet.util.realm.nowCompat
import app.eluvio.wallet.util.realm.toDate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.TypedRealmObject
import io.realm.kotlin.types.annotations.Ignore
import java.util.Locale
import kotlin.reflect.KClass

class LiveVideoInfoEntity : EmbeddedRealmObject {
    // icon image paths.
    var icons: RealmList<String> = realmListOf()

    var eventStartTime: RealmInstant? = null

    @Ignore
    val eventStarted: Boolean
        get() = (eventStartTime ?: RealmInstant.MIN) <= RealmInstant.nowCompat()

    var _streamStartTime: RealmInstant? = null
    var streamStartTime: RealmInstant?
        get() = _streamStartTime ?: eventStartTime
        set(value) { _streamStartTime = value }

    @Ignore
    val streamStarted: Boolean
        get() = (streamStartTime ?: RealmInstant.MIN) <= RealmInstant.nowCompat()

    var endTime: RealmInstant? = null

    @Ignore
    val ended: Boolean get() = (endTime ?: RealmInstant.MAX) <= RealmInstant.nowCompat()

    @Module
    @InstallIn(SingletonComponent::class)
    object EntityModule {
        @Provides
        @IntoSet
        fun provideEntity(): KClass<out TypedRealmObject> = LiveVideoInfoEntity::class
    }

    override fun toString(): String {
        return "LiveVideoInfoEntity(icons=$icons, eventStartTime=$eventStartTime, streamStartTime=$streamStartTime, endTime=$endTime)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiveVideoInfoEntity

        if (icons != other.icons) return false
        if (eventStartTime != other.eventStartTime) return false
        if (streamStartTime != other.streamStartTime) return false
        if (endTime != other.endTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = icons.hashCode()
        result = 31 * result + (eventStartTime?.hashCode() ?: 0)
        result = 31 * result + (streamStartTime?.hashCode() ?: 0)
        result = 31 * result + (endTime?.hashCode() ?: 0)
        return result
    }

}

fun LiveVideoInfoEntity.getEventStartDateTimeString(context: Context): String? =
    eventStartTime?.toDate()?.let { startTime ->
        val dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "M/d")
        val date = DateFormat.format(dateFormat, startTime).toString()
        val timeFormat = DateFormat.getTimeFormat(context)
        val time = timeFormat.format(startTime)
        return "$date at $time"
    }
