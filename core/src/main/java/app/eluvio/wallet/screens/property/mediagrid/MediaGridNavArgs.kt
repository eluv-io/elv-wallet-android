package app.eluvio.wallet.screens.property.mediagrid

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.eluvio.wallet.data.GridContentOverride
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.MediaGridTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class MediaGridNavArgs(
    val permissionContext: PermissionContext,
    val gridContentOverride: GridContentOverride? = null,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): MediaGridNavArgs =
        handle.toRoute(MediaGridTypeMap)
}
