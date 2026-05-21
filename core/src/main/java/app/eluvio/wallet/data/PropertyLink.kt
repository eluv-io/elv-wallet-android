package app.eluvio.wallet.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Immutable
data class PropertyLink(
    val id: String,
    val name: String,
    val isCurrent: Boolean,
) : Parcelable
