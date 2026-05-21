package app.eluvio.wallet.network.api.fabric

import app.eluvio.wallet.network.dto.AssetLinkDto
import com.squareup.moshi.JsonClass
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface MarketplaceApi : FabricApi {
    @GET("q/{marketplace}/meta/public/asset_metadata")
    fun getMarketplaceInfo(@Path("marketplace") marketplace: String): Single<MarketplaceDto>
}

// Yes, this is a lot of DTOs just to get a single deeply-nested field, but the alternative is
// manually messing around json parsing and in the end we still need to feed it thru Moshi to
// properly parse the AssetLinkDto, so what's 4 more DTOs+JsonAdapters? :)
@JsonClass(generateAdapter = true)
data class MarketplaceDto(val info: MarketplaceInfoDto?)

@JsonClass(generateAdapter = true)
data class MarketplaceInfoDto(val branding: MarketplaceBrandingDto?)

@JsonClass(generateAdapter = true)
data class MarketplaceBrandingDto(val tv: MarketplaceTvResourcesDto?)

@JsonClass(generateAdapter = true)
data class MarketplaceTvResourcesDto(val logo: AssetLinkDto?)
