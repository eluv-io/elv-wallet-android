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
    // The PageID of the source page that navigated to this page.
    // Used to display the correct background image.
    val sourcePageId: String? = null,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun SavedStateHandle.provide(): UpcomingVideoNavArgs = navArgs()
}
