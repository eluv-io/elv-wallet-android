package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.core.Flowable

fun <A, B, C : Any> Flowable<Pair<A, B>>.combineLatest(other: Flowable<C>): Flowable<Triple<A, B, C>> {
    return Flowable.combineLatest(this, other) { (first, second), third ->
        Triple(first, second, third)
    }
}
