package app.eluvio.wallet.data.stores

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.util.datastore.ReadWritePref
import app.eluvio.wallet.util.datastore.StoreOperation
import app.eluvio.wallet.util.rx.Optional
import io.reactivex.rxjava3.processors.BehaviorProcessor

/**
 * A simple in-memory implementation of [TokenStore] for testing purposes.
 * Left open for mockito spying.
 */
open class InMemoryTokenStore : TokenStore {
    private inner class Pref<T : Any> : ReadWritePref<T> {
        private val subject = BehaviorProcessor.create<Optional<T>>()
        override fun observe() = subject
        override fun get() = subject.value?.orDefault(null)
        override fun set(value: T?, editor: MutablePreferences) = set(value)
        override fun set(value: T?) = subject.onNext(Optional.of(value))
    }

    override val idToken: ReadWritePref<String> = Pref()
    override val accessToken: ReadWritePref<String> = Pref()
    override val refreshToken: ReadWritePref<String> = Pref()
    override val clusterToken: ReadWritePref<String> = Pref()
    override val fabricToken: ReadWritePref<String> = Pref()
    override val fabricTokenExpiration: ReadWritePref<String> = Pref()
    override val walletAddress: ReadWritePref<String> = Pref()
    override val userEmail: ReadWritePref<String> = Pref()

    private val lpPref = Pref<LoginProviders>()
    override var loginProvider: LoginProviders
        get() = lpPref.get() ?: LoginProviders.ORY
        set(value) = lpPref.set(value)

    override val auth0Domain: ReadWritePref<String> = Pref()
    override val auth0ClientId: ReadWritePref<String> = Pref()

    override fun update(vararg operations: StoreOperation) {
        val noOpMap = mutablePreferencesOf()
        operations.forEach { it(noOpMap) }
    }

    override fun wipe() {
        update(
            idToken to null,
            accessToken to null,
            refreshToken to null,
            clusterToken to null,
            fabricToken to null,
            fabricTokenExpiration to null,
            walletAddress to null,
            userEmail to null,
            lpPref to null
        )
    }
}
