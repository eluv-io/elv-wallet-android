package app.eluvio.wallet.screens.qrdialogs.generic

import androidx.lifecycle.SavedStateHandle
import com.ramcosta.composedestinations.generated.navArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

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
    fun provide(handle: SavedStateHandle): FullscreenQRDialogNavArgs = handle.navArgs()
}
