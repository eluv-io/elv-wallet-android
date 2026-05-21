package app.eluvio.wallet.network.converters

import app.eluvio.wallet.data.entities.RedeemStateEntity
import app.eluvio.wallet.network.api.authd.RedeemableOffersApi
import app.eluvio.wallet.network.dto.NftInfoDto
import app.eluvio.wallet.network.dto.NftRedeemableOfferDto
import app.eluvio.wallet.network.dto.RedemptionStatusDto
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.toRealmInstant

fun NftInfoDto.toRedeemStateEntities(
    statuses: List<RedemptionStatusDto>,
    currentUserAddress: String?
): List<RedeemStateEntity> {
    val contract = contract_addr.removePrefix("0x")
    val relevantStatuses = statuses.filter {
        it.operation == RedeemableOffersApi.REDEEM_OPERATION &&
                it.contract == contract &&
                it.tokenId == token_id_str
    }
    return offers?.map { offer -> offer.toEntity(relevantStatuses, currentUserAddress) }.orEmpty()
}

/**
 *[statuses] should be pre-filtered to only include relevant statuses
 */
private fun NftRedeemableOfferDto.toEntity(
    statuses: List<RedemptionStatusDto>,
    currentUserAddress: String?
): RedeemStateEntity {
    val dto = this
    val statusDto = statuses.firstOrNull { it.offerId == dto.id }

    return RedeemStateEntity().apply {
        offerId = dto.id
        active = dto.active
        redeemer = dto.redeemer
        redeemed = dto.redeemed?.toRealmInstant()
        transaction = dto.transaction
        status = parseRedemptionStatus(redeemer, currentUserAddress, statusDto)
    }
}

private fun parseRedemptionStatus(
    redeemer: String?,
    currentUserAddress: String?,
    statusDto: RedemptionStatusDto?
): RedeemStateEntity.RedeemStatus {
    val statusStr = statusDto?.status
    return when {
        redeemer != null -> {
            if (isOwnedByAnotherUser(redeemer, currentUserAddress)) {
                RedeemStateEntity.RedeemStatus.REDEEMED_BY_ANOTHER_USER
            } else {
                RedeemStateEntity.RedeemStatus.REDEEMED_BY_CURRENT_USER
            }
        }

        statusDto == null -> {
            // Unless redemption has occurred in the last 24h, it won't be found in the statuses.
            // This is normal and expected for most Offers.
            RedeemStateEntity.RedeemStatus.UNREDEEMED
        }

        // TODO: we are assuming that when status=complete but no [redeemer], the current user is the owner. is this assumptions true?
        statusStr == "complete" -> RedeemStateEntity.RedeemStatus.REDEEMED_BY_CURRENT_USER
        statusStr == "failed" -> RedeemStateEntity.RedeemStatus.REDEEM_FAILED
        statusStr == "" || statusStr == null -> {
            // The BE sends "" to signal that the redemption is in progress. Don't ask me why.
            // catching nulls just in case the BE starts omitting "" from its responses, resulting in [null] in our DTO.
            RedeemStateEntity.RedeemStatus.REDEEMING
        }

        else -> {
            // There *is* a redemption status, but it's not one we recognize.
            Log.w("Unknown redemption status: ${statusDto.status}. Defaulting to UNREDEEMED")
            RedeemStateEntity.RedeemStatus.REDEEMING
        }
    }
}

private fun isOwnedByAnotherUser(redeemer: String?, currentUser: String?): Boolean {
    val redeemerAddress = redeemer?.removePrefix("0x")?.lowercase() ?: return false
    val currentAddress = currentUser?.removePrefix("0x")?.lowercase() ?: return false
    return redeemerAddress.isNotEmpty() && redeemerAddress != currentAddress
}
