package app.eluvio.wallet.screens.qrdialogs.generic

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class FullscreenQRDialogNavArgs(
    val url: String,
    val title: String,
    val subtitleOverride: String? = null,
    val shortenUrl: Boolean = true
) : NavKey
