package app.eluvio.wallet.screens.property.upcoming

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.screens.navArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

data class UpcomingVideoNavArgs(
    val propertyId: String,
    val mediaItemId: String,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun SavedStateHandle.provide(): UpcomingVideoNavArgs = navArgs()
}
