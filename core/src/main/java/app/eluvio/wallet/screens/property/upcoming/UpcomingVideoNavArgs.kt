package app.eluvio.wallet.screens.property.upcoming

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
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
    fun provide(handle: SavedStateHandle): UpcomingVideoNavArgs = handle.toRoute()
}
