package app.eluvio.wallet.screens.nftdetail.legacy

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class LockedMediaDialogNavArgs(
    val name: String,
    val imageUrl: String,
    val subtitle: String? = null,
    val aspectRatio: Float,
) : NavKey
