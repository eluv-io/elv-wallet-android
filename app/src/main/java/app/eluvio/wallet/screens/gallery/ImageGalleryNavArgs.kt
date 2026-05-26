package app.eluvio.wallet.screens.gallery

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ImageGalleryNavArgs(val mediaEntityId: String) : NavKey
