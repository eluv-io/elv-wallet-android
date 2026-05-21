package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.FulfillmentDataEntity
import app.eluvio.wallet.data.entities.NftEntity
import app.eluvio.wallet.data.entities.RedeemStateEntity
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.authd.RedeemableOffersApi
import app.eluvio.wallet.network.converters.toEntity
import app.eluvio.wallet.network.converters.toRedeemStateEntities
import app.eluvio.wallet.network.dto.InitiateRedemptionRequest
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveTo
import app.eluvio.wallet.util.realm.toRealmListOrEmpty
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FulfillmentStore @Inject constructor(
    private val apiProvider: ApiProvider,
    private val envStore: EnvironmentStore,
    private val realm: Realm,
    private val tokenStore: TokenStore,
) {

    /**
     * Fetches nft/info and checks redemption status for each offer. Returns the updated nft.
     */
    fun refreshRedeemedOffers(nft: NftEntity): Single<NftEntity> {
        return apiProvider.getApi(RedeemableOffersApi::class)
            .flatMap { api ->
                api.getNftInfo(nft.contractAddress, nft.tokenId)
                    .flatMap { nftInfo ->
                        api.getRedemptionStatus(nftInfo.tenant).map { statuses ->
                            nftInfo.tenant to nftInfo.toRedeemStateEntities(
                                statuses,
                                tokenStore.walletAddress.get()
                            )
                        }
                    }
            }
            .map { (tenant, redeemStates) ->
                // TODO: untested code. We changed realm entities to be unmanaged, so we can't just
                //  pass [nft] to [findLatest] anymore. We need to find the latest nft "manually"
                realm.writeBlocking {
                    query<NftEntity>("${NftEntity::id.name} == $0", nft.id)
                        .first().find()
                        ?.also {
                            // Filter any offers that don't have valid redeem states
                            it.redeemableOffers.removeAll { offer -> redeemStates.none { redeemState -> redeemState.offerId == offer.offerId } }
                            it.redeemStates = redeemStates.toRealmListOrEmpty()
                            it.nftTemplate?.tenant = tenant
                        }
                } ?: nft
            }
    }

    fun loadFulfillmentData(transactionHash: String): Completable {
        return apiProvider.getApi(RedeemableOffersApi::class)
            .zipWith(envStore.observeSelectedEnvironment().firstOrError())
            .flatMap { (api, env) ->
                api.getFulfillmentData(env.networkName, transactionHash)
            }
            .map { it.toEntity(transactionHash) }
            .saveTo(realm)
            .ignoreElement()
    }

    /**
     * Observes fulfillment information from db. If it's not there, fetches it from the server.
     */
    fun observeFulfillmentData(transactionHash: String) =
        realm.query<FulfillmentDataEntity>(
            "${FulfillmentDataEntity::transactionHash.name} = $0",
            transactionHash
        )
            .asFlowable()
            .switchMap {
                val data = it.firstOrNull()
                if (data == null) {
                    Log.d("Fulfillment data not found, fetching from server")
                    loadFulfillmentData(transactionHash).andThen(Flowable.empty())
                } else {
                    Flowable.just(data)
                }
            }

    fun initiateRedemption(
        nft: NftEntity,
        offerId: String,
        reference: String,
    ): Completable {
        val tenant =
            nft.nftTemplate?.tenant
                ?: return Completable.error(IllegalStateException("NFT has no tenant"))
        val request =
            InitiateRedemptionRequest(reference, nft.contractAddress, nft.tokenId, offerId.toInt())
        return Completable.fromAction {
            // Optimistically set state to REDEEMING
            nft.writeStatusBlocking(offerId, RedeemStateEntity.RedeemStatus.REDEEMING)
            Log.i("Optimistically set offer#${offerId} status to REDEEMING")
        }
            .andThen(apiProvider.getApi(RedeemableOffersApi::class))
            .flatMapCompletable { api -> api.initiateRedemption(tenant, request) }
            .doOnError {
                // Revert optimism
                nft.writeStatusBlocking(offerId, RedeemStateEntity.RedeemStatus.UNREDEEMED)
                Log.e("Request failed, reverted offer#${offerId} status to UNREDEEMED")
            }
    }

    /**
     * Convenience method to update the status of an offer in Realm
     */
    private fun NftEntity.writeStatusBlocking(
        offerId: String,
        status: RedeemStateEntity.RedeemStatus
    ) {
        val nft = this
        // TODO: untested code. We changed realm entities to be unmanaged, so we can't just
        //  pass [nft] to [findLatest] anymore. We need to find the latest nft "manually"
        realm.writeBlocking {
            query<NftEntity>("${NftEntity::id.name} == $0", nft.id)
                .first().find()
                ?.also {
                    it.redeemStates
                        .first { offer -> offer.offerId == offerId }
                        .status = status
                }
        }
    }
}
