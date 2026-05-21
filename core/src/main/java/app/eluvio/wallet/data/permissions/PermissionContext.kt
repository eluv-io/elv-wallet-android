package app.eluvio.wallet.data.permissions

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.SectionItemEntity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy for a specific item.
 * This is mostly needed for the purchase flow.
 */
@Parcelize
@Serializable
@Immutable
data class PermissionContext(
    val propertyId: String,
    val pageId: String? = null,
    val sectionId: String? = null,
    val sectionItemId: String? = null,
    val mediaItemId: String? = null,
) : Parcelable {
    /**
     * A resolved version of the context, where every defined ID is resolved to the actual entity.
     */
    data class Resolved(
        val property: MediaPropertyEntity,
        val page: MediaPageEntity? = null,
        val section: MediaPageSectionEntity? = null,
        val sectionItem: SectionItemEntity? = null,
        val mediaItem: MediaEntity? = null
    )
}
