package app.eluvio.wallet.testing

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber

/**
 * A JUnit rule that plants a Timber tree that logs to stdout instead of Logcat, so outputs can be seen in unit tests.
 */
class TestLogRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)

        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false

        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println(message)
            }
        })
    }
}
