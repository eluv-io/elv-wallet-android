package app.eluvio.wallet.app

interface Events {
    open class ToastMessage(val message: String) : Events

    data object NetworkError : ToastMessage("Network error. Please try again.")

    data object NftNotFound : ToastMessage("You don't own this NFT")
}
