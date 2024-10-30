package app.eluvio.wallet.util.realm

import android.os.Build
import io.realm.kotlin.internal.RealmInstantImpl
import io.realm.kotlin.internal.platform.currentTime
import io.realm.kotlin.types.RealmInstant
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [currentTime], and by extension [RealmInstant.now], has a bug where devices running API 25 and below will return the
 * wrong time. This function will return the current time in a way that is compatible with all devices.
 *
 * @see <a href="https://github.com/realm/realm-kotlin/issues/1849">realm-kotlin #1849</a>
 */
fun RealmInstant.Companion.nowCompat(): RealmInstant {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val jtInstant = java.time.Clock.systemUTC().instant()
        RealmInstantImpl(jtInstant.epochSecond, jtInstant.nano)
    } else {
        val now = System.currentTimeMillis()
        RealmInstantImpl(now / 1000, (now % 1000).toInt() * 1_000_000)
    }
}

/**
 * Converts a [RealmInstant] to a [Date].
 */
fun RealmInstant.toDate() = Date(this.millis)

/**
 * Only accurate to seconds (when minSdk >= 26 we can use LocalDateTime instead).
 */
fun Date.toRealmInstant(): RealmInstant {
    return RealmInstant.from(time / 1000, 0)
}

val RealmInstant.millis: Long
    get() = (epochSeconds.seconds + nanosecondsOfSecond.nanoseconds).inWholeMilliseconds
