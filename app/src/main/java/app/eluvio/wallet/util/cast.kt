package app.eluvio.wallet.util

/**
 * Convenience method that makes [as?] calls easier to chain.
 */
inline fun <reified T : Any> Any.cast(): T? = this as? T
