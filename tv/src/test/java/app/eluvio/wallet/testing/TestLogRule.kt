package app.eluvio.wallet.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber

/**
 * A JUnit rule that plants a Timber tree that logs to stdout instead of Logcat, so outputs can be seen in unit tests.
 */
class TestLogRule : TestWatcher() {
    override fun starting(description: Description?) {
        // Skip if we already planted
        if (Timber.treeCount > 0) return
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println(message)
            }
        })
    }
}
