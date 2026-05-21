package app.eluvio.wallet.network.dto

import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class NftInfoDto(
    val contract_addr: String,
    val offers: List<NftRedeemableOfferDto>?,
    val tenant: String,//e.g. "iten4TXq2en3qtu3JREnE5tSLRf9zLod"
    val token_id_str: String,
    val token_owner: String, //e.g. "0xe05Ac81248A7e9A08678Ee7756CC72219955653f"
)

@JsonClass(generateAdapter = true)
data class NftRedeemableOfferDto(
    val id: String,
    val active: Boolean,
    val redeemer: String?,
    val redeemed: Date?,
    val transaction: String?,
)
