package app.eluvio.wallet.screens.nftdetail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class NftDetailNavArgs(
    val contractAddress: String,
    val tokenId: String,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): NftDetailNavArgs = handle.toRoute()
}
