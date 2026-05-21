package app.eluvio.wallet.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FabricConfiguration(
    @field:Json(name = "node_id") val nodeID: String,
    @field:Json(name = "network") val network: Network,
    @field:Json(name = "qspace") val qspace: QSpace,
) {
    // TODO: replace with failover logic (probably in a new component)
    val fabricEndpoint: String = "${network.services.fabricApi.first()}/s/${qspace.names.first()}"

    // TODO: replace with failover logic (probably in a new component)
    val authdEndpoint: String = network.services.authService.first()
}

@JsonClass(generateAdapter = true)
data class Network(
    @field:Json(name = "services") val services: Services
)

@JsonClass(generateAdapter = true)
data class Services(
    @field:Json(name = "authority_service") val authService: List<String>,
    @field:Json(name = "ethereum_api") val ethereumApi: List<String>,
    @field:Json(name = "fabric_api") val fabricApi: List<String>,
)

@JsonClass(generateAdapter = true)
data class QSpace(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "names") val names: List<String>
)
