package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single


/**
 * Returns an Observable that applies a specified function to each item emitted by the source
 * ObservableSource and emits the results of these function applications,
 * for every result that isn't [null]. [null]s will be filtered out.
 *
 * Makes RxJava and Kotlin play nice so type becomes non-nullable (Any? -> Any)
 * and only emits when it succeeds
 */
fun <T : Any, R : Any> Observable<T>.mapNotNull(mapper: (T) -> R?): Observable<R> = flatMap {
    val result = mapper(it)
    if (result != null) {
        Observable.just(result)
    } else {
        Observable.empty()
    }
}

/**
 * Returns a Flowable that applies a specified function to each item emitted by the source
 * Publisher and emits the results of these function applications,
 * for every result that isn't [null]. [null]s will be filtered out.
 *
 * Makes RxJava and Kotlin play nice so type becomes non-nullable (Any? -> Any)
 * and only emits when it succeeds
 */
fun <T : Any, R : Any> Flowable<T>.mapNotNull(mapper: (T) -> R?): Flowable<R> {
    return flatMap {
        val result = mapper(it)
        if (result == null) {
            Flowable.empty()
        } else {
            Flowable.just(result)
        }
    }
}

/**
 * Returns an Observable that applies a specified function to each item emitted by the source
 * ObservableSource and emits the results of these function applications,
 * for every result that isn't [null]. [null]s will be filtered out.
 *
 * Makes RxJava and Kotlin play nice so type becomes non-nullable (Any? -> Any)
 * and only emits when it succeeds
 */
fun <T : Any, R : Any> Single<T>.mapNotNull(mapper: (T) -> R?): Maybe<R> = flatMapMaybe {
    val result = mapper(it)
    if (result != null) {
        Maybe.just(result)
    } else {
        Maybe.empty()
    }
}

fun <T : Any, R : Any> Maybe<T>.mapNotNull(mapper: (T) -> R?): Maybe<R> = flatMap {
    val result = mapper(it)
    if (result != null) {
        Maybe.just(result)
    } else {
        Maybe.empty()
    }
}
