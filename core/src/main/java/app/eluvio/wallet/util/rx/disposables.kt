package app.eluvio.wallet.util.rx

import io.reactivex.rxjava3.disposables.Disposable

/**
 * A no-op method that eliminates the warning about unused result, in an expressive manner.
 */
fun Disposable.unsaved() = this

fun Disposable?.safeDispose() {
    this?.takeIf { !it.isDisposed }?.dispose()
}
