package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable


/**
 * Converts [Flowable]<[T]> to [Flowable]<[Optional]<[R]>> by given selector.
 * If selector returns null, [Optional] will be empty.
 *
 * (mapOptional is a better name, but already used by RxJava)
 */
fun <T : Any, R:Any> Flowable<T>.optionalMap(mapper: (T) -> R?): Flowable<Optional<R>> =
    map { Optional.of(mapper(it)) }
