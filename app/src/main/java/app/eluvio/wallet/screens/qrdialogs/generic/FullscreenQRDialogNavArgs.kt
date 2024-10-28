package app.eluvio.wallet.screens.qrdialogs.generic

import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.screens.navArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

data class FullscreenQRDialogNavArgs(
    // Either a fully qualified URL, or a path (starting with "/") to append to the wallet URL.
    val urlOrWalletPath: String,
    val title: String,
    val subtitleOverride: String? = null,
    val shortenUrl: Boolean = true,
    val appendAuthToken: Boolean = false,
)

@Module
@InstallIn(ViewModelComponent::class)
object NavArgModule {
    @Provides
    fun SavedStateHandle.provide(): FullscreenQRDialogNavArgs = navArgs()
}
