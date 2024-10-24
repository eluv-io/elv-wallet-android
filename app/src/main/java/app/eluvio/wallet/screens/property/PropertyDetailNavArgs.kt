package app.eluvio.wallet.screens.property

data class PropertyDetailNavArgs(
    val propertyId: String,
    /** Only required to navigate to a specific page. Usually due to showAltPage permission behavior */
    val pageId: String? = null,
    val propertyLinks: ArrayList<DynamicPageLayoutState.PropertyLink> = arrayListOf(),
)
