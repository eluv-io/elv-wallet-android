package app.eluvio.wallet.network.dto

import androidx.annotation.Keep
import app.eluvio.wallet.data.entities.ContractInfoEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class ContractInfoDto(
    override val contract: String,
    override val cap: Int,
    override val minted: Int,
    @field:Json(name = "total_supply")
    override val totalSupply: Int,
    override val burned: Int,
) : ContractInfoEntity
