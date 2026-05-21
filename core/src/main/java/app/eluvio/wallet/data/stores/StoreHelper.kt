package app.eluvio.wallet.data.stores

import app.eluvio.wallet.util.rx.zipWithGenerator
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Observe a Realm query and fetch data from the network when needed.
 * @param realmQuery The query to observe continuously.
 * @param fetchOperation A producer of the network fetch operation.
 *  return 'null' if no fetch is needed.
 *  It is up to the caller to save this data to Realm as part of the Completable operation.
 */
fun <T : Any> observeRealmAndFetch(
    realmQuery: Flowable<T>,
    fetchOperation: (localState: T, isFirstState: Boolean) -> Completable?,
): Flowable<T> {
    return realmQuery
        // Whenever the DB emits, this will combine with [true] value for the first item, but
        // [false] for the rest. That way we can hit the network only once, rather than every time
        // the DB emits.
        .zipWithGenerator(true) { false }
        .distinctUntilChanged(
            // This avoids an infinite loop when we can't fetch all media in one page,
            // because until we implement pagination, there will always be missing media.
        )
        .switchMap { (queryResult, isFirstState) ->
            Flowable.just(queryResult)
                .mergeWith(
                    fetchOperation(queryResult, isFirstState) ?: Completable.complete()
                )
        }
}
