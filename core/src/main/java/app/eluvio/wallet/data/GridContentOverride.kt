package app.eluvio.wallet.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class GridContentOverride(
    val title: String,
    // ArrayList rather than List for compose-destinations Parcelable nav-arg encoding.
    val mediaItemsOverride: ArrayList<String> = arrayListOf(),
) : Parcelable
