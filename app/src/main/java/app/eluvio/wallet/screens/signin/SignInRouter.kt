package app.eluvio.wallet.screens.signin

import androidx.compose.runtime.Composable
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.navigation.AuthFlowGraph
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.asReplace
import com.ramcosta.composedestinations.generated.destinations.Auth0SignInDestination
import com.ramcosta.composedestinations.generated.destinations.OrySignInDestination
import com.ramcosta.composedestinations.annotation.Destination

@Destination<AuthFlowGraph>(start = true, navArgs = SignInNavArgs::class)
@Composable
fun SignInRouter(navArgs: SignInNavArgs) {
    val navigator = LocalNavigator.current
    when (navArgs.provider) {
        LoginProviders.AUTH0 -> Auth0SignInDestination(navArgs)
        LoginProviders.ORY -> OrySignInDestination(navArgs)
    }.let { navigator(it.asReplace()) }
}
