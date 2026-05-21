package app.eluvio.wallet.screens.qrdialogs.externalmedia

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ExternalMediaQrDialogNavArgs(val mediaItemId: String) : NavKey
