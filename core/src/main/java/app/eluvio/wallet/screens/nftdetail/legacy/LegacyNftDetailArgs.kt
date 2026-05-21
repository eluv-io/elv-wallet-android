package app.eluvio.wallet.screens.nftdetail.legacy

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class LegacyNftDetailArgs(
    val contractAddress: String,
    val tokenId: String,
    val marketplaceId: String? = null,
    val backLink: String? = null,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): LegacyNftDetailArgs = handle.toRoute()
}
