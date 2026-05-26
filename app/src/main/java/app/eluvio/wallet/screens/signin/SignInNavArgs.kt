package app.eluvio.wallet.screens.signin

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.screens.property.PropertyDetailNavArgs
import kotlinx.serialization.Serializable

/**
 * Shared nav args for all sign in modes. They don't all need all the data, but it makes it easier
 * to navigate between them.
 */
@Serializable
data class SignInNavArgs(
    val provider: String,
    val propertyId: String,
    // Where the auth flow should navigate to once successfully signed in. Narrowed to
    // PropertyDetail (the only target used in practice) so the route stays a concrete
    // @Serializable type and doesn't need polymorphic NavKey serialization.
    val onSignedInTarget: PropertyDetailNavArgs? = null,
) : NavKey
