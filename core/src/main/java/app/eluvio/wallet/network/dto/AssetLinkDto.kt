package app.eluvio.wallet.network.dto

import app.eluvio.wallet.data.entities.FabricUrlEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssetLinkDto(
    /**
     * The relative path to the asset. Can be appended on any node endpoint to get the asset.
     */
    val path: String
) {
    /**
     * Converts an [AssetLinkDto] to a full URL, when [AssetLinkDto.path] is not empty.
     */
    fun toUrl(baseUrl: String, imageHash: String? = null): FabricUrlEntity? {
        if (path.isEmpty()) return null
        return FabricUrlEntity().apply {
            set(baseUrl, path, imageHash)
        }
    }
}
