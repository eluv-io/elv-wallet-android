package app.eluvio.wallet.screens.property

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.eluvio.wallet.data.PropertyLink
import app.eluvio.wallet.navigation.PropertyDetailTypeMap
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class PropertyDetailNavArgs(
    val propertyId: String,
    /** Only required to navigate to a specific page. Usually due to showAltPage permission behavior */
    val pageId: String? = null,
    val propertyLinks: ArrayList<PropertyLink> = arrayListOf(),
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): PropertyDetailNavArgs =
        handle.toRoute(PropertyDetailTypeMap)
}
