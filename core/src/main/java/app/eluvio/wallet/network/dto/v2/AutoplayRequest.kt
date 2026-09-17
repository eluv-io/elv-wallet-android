package app.eluvio.wallet.network.dto.v2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Asks the server what plays after [mediaId].
 * [sectionId] resolves groups configured as "<Current Section>", and [mediaListId] tells the
 * server the user came in through a media list. Both are omitted when we don't know them.
 */
@JsonClass(generateAdapter = true)
data class AutoplayRequest(
    @field:Json(name = "media_id")
    val mediaId: String,
    @field:Json(name = "section_id")
    val sectionId: String? = null,
    @field:Json(name = "media_list_id")
    val mediaListId: String? = null,
)
