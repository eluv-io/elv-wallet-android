package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.kotlin.Flowables

/**
 * Returns a cold, synchronous, stateful and backpressure-aware generator of values.
 * When requested by the downstream, the generator emits [initialState] and applies the given function
 * to generate the next value.
 * @see Flowable.generate
 **/
fun <T : Any> Flowables.generate(initialState: T, generator: (T) -> T): Flowable<T> {
    return Flowable.generate<T, T>(
        { initialState },
        BiFunction { state, emitter ->
            emitter.onNext(state)
            return@BiFunction generator(state)
        }
    )
}
