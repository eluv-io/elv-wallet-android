package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.Flowables
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun Flowables.interval(duration: Duration): Flowable<Long> =
    Flowable.interval(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)

fun Flowables.timer(duration: Duration): Flowable<Long> =
    Flowable.timer(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)

fun <T : Any> Flowable<T>.timeout(duration: Duration) =
    timeout(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
