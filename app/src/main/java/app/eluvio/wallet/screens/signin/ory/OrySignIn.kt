package app.eluvio.wallet.screens.signin.ory

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import app.eluvio.wallet.navigation.AuthFlowGraph
import app.eluvio.wallet.screens.signin.SignInNavArgs
import app.eluvio.wallet.screens.signin.common.SignInView
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@Destination<AuthFlowGraph>(navArgs = SignInNavArgs::class)
@Composable
fun OrySignIn() {
    hiltViewModel<OrySignInViewModel>().subscribeToState { vm, state ->
        SignInView(state, onRequestNewToken = vm::requestNewToken, showMetamaskLink = false)
    }
}
