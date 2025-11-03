package app.eluvio.wallet.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.Environment
import app.eluvio.wallet.data.stores.EnvironmentStore
import app.eluvio.wallet.data.stores.FabricConfigStore
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.theme.EluvioTheme
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.util.subscribeToState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject


@AndroidEntryPoint
class EnvSelectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EluvioTheme {
                CompositionLocalProvider(
                    LocalNavigator provides { /*No-Op*/ },
                ) {
                    hiltViewModel<EnvSelectViewModel>().subscribeToState { vm, state ->
                        EnvSelector(state, vm::onEnvSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvSelector(state: EnvSelectViewModel.State, onEnvSelected: (Environment) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.loading) {
            EluvioLoadingSpinner(Modifier.padding(bottom = 20.dp))
        } else {
            Text("Select Environment:", Modifier.padding(bottom = 10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.availableEnvironments.forEach { env ->
                val selected = state.selectedEnv == env
                TvButton(onClick = { onEnvSelected(env) }) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(
                                top = 5.dp,
                                bottom = 5.dp,
                                start = 20.dp,
                                end = if (selected) 8.dp else 20.dp
                            )
                    ) {
                        Text(
                            text = env.name,
                            style = MaterialTheme.typography.label_40,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Clear",
                                Modifier.padding(start = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class EnvSelectViewModel @Inject constructor(
    private val environmentStore: EnvironmentStore,
    private val fabricConfigStore: FabricConfigStore,
    private val signOutHandler: SignOutHandler,
) : BaseViewModel<EnvSelectViewModel.State>(State()) {
    @Immutable
    data class State(
        val loading: Boolean = false,
        val availableEnvironments: List<Environment> = if (BuildConfig.DEBUG) {
            Environment.entries
        } else {
            listOf(Environment.Main)
        },
        val selectedEnv: Environment? = null
    )

    override fun onResume() {
        super.onResume()

        environmentStore.observeSelectedEnvironment()
            .subscribeBy {
                updateState { copy(selectedEnv = it) }
            }
            .addTo(disposables)
    }

    fun onEnvSelected(env: Environment) {
        fabricConfigStore.setEnvAndAwaitNewConfig(env)
            .doOnSubscribe { updateState { copy(loading = true) } }
            .andThen(signOutHandler.signOut("Env changed, restarting app."))
            .subscribeBy(onComplete = { updateState { copy(loading = false) } })
            .addTo(disposables)
    }
}
