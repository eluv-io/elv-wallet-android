package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable

fun <T : Any> Observable<T>.asSharedState() = replay(1).refCount().distinctUntilChanged()
fun <T : Any> Flowable<T>.asSharedState() = replay(1).refCount().distinctUntilChanged()
