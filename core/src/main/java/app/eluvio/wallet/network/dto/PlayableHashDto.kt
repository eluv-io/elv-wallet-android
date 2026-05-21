package app.eluvio.wallet.network.dto

/**
 * A container for "playable" hashes. Usually Video/Audio objects that will be passed to the
 * Playout API. This is different from [AssetLinkDto], where the link is a direct URL to the asset/file.
 */
data class PlayableHashDto(val hash: String)
