package app.eluvio.wallet.data.entities

// We don't cache this data, so we'll just pass the DTO along since it needs no transformation
interface ContractInfoEntity {
    val contract: String
    val cap: Int
    val minted: Int
    val totalSupply: Int
    val burned: Int
}
