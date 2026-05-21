package app.eluvio.wallet.screens.signin

import com.ramcosta.composedestinations.spec.Direction

/**
 * Shared nav args for all sign in modes. They don't all need all the data, but it makes it easier
 * to navigate between them.
 */
data class SignInNavArgs(
    val provider: String,
    val propertyId: String,
    // Where the auth flow should navigate to once successfully signed in
    val onSignedInDirection: Direction? = null
)
