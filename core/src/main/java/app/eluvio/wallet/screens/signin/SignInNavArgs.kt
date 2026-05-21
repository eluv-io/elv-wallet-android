package app.eluvio.wallet.screens.signin

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.eluvio.wallet.navigation.NavTarget
import app.eluvio.wallet.navigation.SignInTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

/**
 * Shared nav args for all sign in modes. They don't all need all the data, but it makes it easier
 * to navigate between them.
 */
@Serializable
data class SignInNavArgs(
    val provider: String,
    val propertyId: String,
    // Where the auth flow should navigate to once successfully signed in
    val onSignedInTarget: NavTarget? = null,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): SignInNavArgs =
        handle.toRoute(SignInTypeMap)
}
