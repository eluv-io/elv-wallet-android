package app.eluvio.wallet.screens.qrdialogs.generic

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.Serializable

@Serializable
data class FullscreenQRDialogNavArgs(
    val url: String,
    val title: String,
    val subtitleOverride: String? = null,
    val shortenUrl: Boolean = true
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun provide(handle: SavedStateHandle): FullscreenQRDialogNavArgs = handle.toRoute()
}
