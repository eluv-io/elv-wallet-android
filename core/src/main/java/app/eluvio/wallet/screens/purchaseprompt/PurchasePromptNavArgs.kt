package app.eluvio.wallet.screens.purchaseprompt

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.navigation.PurchasePromptTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class PurchasePromptNavArgs(
    val permissionContext: PermissionContext,
    /**
     *  When provided, will just prompt to view the specified pageId, without including a "context" param for the web client
     */
    val pageOverride: String? = null,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): PurchasePromptNavArgs =
        handle.toRoute(PurchasePromptTypeMap)
}
