package app.eluvio.wallet.navigation

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootGraph

/**
 * Define navigation graphs in the app.
 */

@NavGraph<RootGraph>(start = true)
annotation class MainGraph

@NavGraph<RootGraph>
annotation class AuthFlowGraph
