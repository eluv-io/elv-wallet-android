package app.eluvio.wallet.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class PropertyLink(
    val id: String,
    val name: String,
    val isCurrent: Boolean,
)
