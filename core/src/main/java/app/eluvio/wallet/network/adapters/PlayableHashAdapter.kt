package app.eluvio.wallet.network.adapters

import app.eluvio.wallet.network.dto.PlayableHashDto
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class PlayableHashAdapter {
    @FromJson
    fun fromJson(link: Map<String, *>): PlayableHashDto? {
        val hash = (link["."] as? Map<*,*>)
            ?.get("source")?.toString()
            ?: link["/"]?.toString()
                ?.split("/")
                ?.firstOrNull { it.startsWith("hq__") }
        return hash?.let { PlayableHashDto(hash) }
    }

    @ToJson
    fun toJson(dto: PlayableHashDto): String {
        throw UnsupportedOperationException("Can't convert PlayableHashDto to JSON")
    }
}
