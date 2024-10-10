package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

/**
 * Asynchronously performs [action] on the success value of the [Single], and then emits the original value.
 */
fun <T : Any> Single<T>.doOnSuccessAsync(action: (T) -> Completable): Single<T> = flatMap {
    action(it).andThen(Single.just(it))
}

/**
 * Asynchronously performs [action] on the success value of the [Maybe], and then emits the original value.
 */
fun <T : Any> Maybe<T>.doOnSuccessAsync(action: (T) -> Completable): Maybe<T> = flatMap {
    action(it).andThen(Maybe.just(it))
}
