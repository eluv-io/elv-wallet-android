package app.eluvio.wallet.network.converters

import app.eluvio.wallet.data.entities.FulfillmentDataEntity
import app.eluvio.wallet.network.dto.FulfillmentResponseDto

fun FulfillmentResponseDto.toEntity(transaction: String): FulfillmentDataEntity {
    val dto = this
    return FulfillmentDataEntity().apply {
        transactionHash = transaction
        message = dto.message
        url = dto.fulfillment_data?.url
        code = dto.fulfillment_data?.code
    }
}
