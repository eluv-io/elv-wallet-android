package app.eluvio.wallet.data

import kotlinx.serialization.Serializable

@Serializable
data class GridContentOverride(
    val title: String,
    val mediaItemsOverride: List<String> = emptyList(),
)
