package app.eluvio.wallet.debug

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.activity.ComponentActivity
import app.eluvio.wallet.core.R
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.Environment
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.FabricConfigStore
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject

@AndroidEntryPoint
class EnvSelectActivity : ComponentActivity() {

    @Inject lateinit var environmentStore: EnvironmentStore
    @Inject lateinit var fabricConfigStore: FabricConfigStore
    @Inject lateinit var signOutHandler: SignOutHandler

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_env_select)

        val envGroup = findViewById<RadioGroup>(R.id.env_group)
        val stagingSwitch = findViewById<Switch>(R.id.staging_switch)

        environmentStore.observeSelectedEnvironment()
            .firstElement()
            .subscribeBy { current ->
                Environment.entries.forEach { env ->
                    envGroup.addView(RadioButton(this).apply {
                        text = env.name
                        tag = env
                        isChecked = env == current
                    })
                }
                envGroup.setOnCheckedChangeListener { group, id ->
                    val env = group.findViewById<RadioButton>(id)?.tag as? Environment
                    if (env != null && env != current) applyEnv(env)
                }
            }
            .addTo(disposables)

        stagingSwitch.isChecked = environmentStore.stagingFlag.get() == true
        stagingSwitch.setOnCheckedChangeListener { _, checked ->
            environmentStore.stagingFlag.set(checked)
        }
    }

    private fun applyEnv(env: Environment) {
        fabricConfigStore.setEnvAndAwaitNewConfig(env)
            .andThen(signOutHandler.signOut("Env changed, restarting app."))
            .subscribeBy(onError = {})
            .addTo(disposables)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
