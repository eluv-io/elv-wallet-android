package app.eluvio.wallet.data

import app.eluvio.wallet.data.entities.FabricUrlEntity

/**
 * Represents a URL to fabric content.
 *
 * There are a few reasons we add the complication of this interface instead of just using raw strings:
 * 1. It was getting hard to tell what was a URL and what was a path.
 * 2. ViewModels needed to keep fetching the fabric base url to pass down to views.
 * 3. We wanted to be able to change the base URL across the entire Realm database when the config.
 * 4. We don't want to compare base url for entity equality checks, and having a separate class
 *    makes that easier.
 *
 * @see FabricUrlEntity
 */
interface FabricUrl {
    val url: String?
    val imageHash: String?
}
