package app.eluvio.wallet.screens.property.mediagrid

import android.os.Parcelable
import app.eluvio.wallet.data.permissions.PermissionContext
import kotlinx.parcelize.Parcelize

data class MediaGridNavArgs(
    val permissionContext: PermissionContext,
    val gridContentOverride: GridContentOverride? = null
)

@Parcelize
data class GridContentOverride(
    val title: String,
    // Using ArrayList because:
    // 1) List<*> doesn't play well with Compose Navigation.
    // 2) Array<*> doesn't play well with data classes' automatic equals/hashCode implementation.
    val mediaItemsOverride: ArrayList<String> = arrayListOf()
) : Parcelable
