package app.eluvio.wallet.screens.home

import androidx.navigation3.runtime.NavKey
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import kotlinx.serialization.Serializable

@Serializable
data class DeeplinkArgs(
    val action: String? = null,
    val marketplace: String? = null,
    val contract: String? = null,
    val sku: String? = null,
    val jwt: String? = null,
    val entitlement: String? = null,
    val backLink: String? = null,
) : NavKey {
    fun toDeeplinkRequest(): DeeplinkRequestEntity? {
        val entity = this
        return DeeplinkRequestEntity().apply {
            this.action = entity.action ?: return null
            this.marketplace = entity.marketplace ?: return null
            this.contract = entity.contract ?: return null
            this.sku = entity.sku ?: return null
            this.jwt = entity.jwt
            this.entitlement = entity.entitlement
            this.backLink = entity.backLink
        }
    }
}
