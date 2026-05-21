package app.eluvio.wallet.data.stores

import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.NftClaimApi
import app.eluvio.wallet.network.dto.InitiateEntitlementClaimRequest
import app.eluvio.wallet.network.dto.InitiateNftClaimRequest
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class NftClaimStore @Inject constructor(
    private val apiProvider: ApiProvider
) {
    sealed interface NftClaimResult {
        object Pending : NftClaimResult
        data class Success(val contractAddress: String, val tokenId: String) : NftClaimResult
        //TODO: add error case?
    }

    /**
     * Claim an NFT for the given tenant/marketplace/sku.
     * Returns the "operation" key to use for checking the status of the claim.
     */
    fun initiateNftClaim(
        tenant: String, marketplaceId: String, sku: String, entitlement: String?
    ): Single<String> {
        return apiProvider.getApi(NftClaimApi::class)
            .flatMap { api ->
                if (entitlement != null) {
                    api.claimEntitlement(tenant, InitiateEntitlementClaimRequest(entitlement))
                } else {
                    api.claimNft(tenant, InitiateNftClaimRequest(marketplaceId, sku))
                }
            }
            .map { it.operationKey }
    }

    /**
     * @param op The operation key returned by [initiateNftClaim]
     */
    fun checkNftClaimStatus(tenant: String, op: String): Single<NftClaimResult> {
        return apiProvider.getApi(NftClaimApi::class)
            .flatMap { api -> api.getClaimStatus(tenant) }
            .map { dto ->
                val extra = dto.firstOrNull { it.operationKey == op }?.extra
                when {
                    extra == null -> NftClaimResult.Pending
                    extra.claimResult == null -> throw IllegalStateException("nft-claim has \"extra\" but no \"claimResult\"")
                    else -> NftClaimResult.Success(
                        contractAddress = extra.claimResult.contractAddress,
                        tokenId = extra.claimResult.tokenId
                    )
                }
            }
    }
}
