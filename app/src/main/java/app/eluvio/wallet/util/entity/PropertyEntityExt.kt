package app.eluvio.wallet.util.entity

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.permissions.PermissionSettingsEntity
import app.eluvio.wallet.data.entities.v2.permissions.showAlternatePage
import app.eluvio.wallet.data.entities.v2.permissions.showPurchaseOptions
import app.eluvio.wallet.data.permissions.PermissionContext
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.util.logging.Log
import io.reactivex.rxjava3.core.Flowable

/**
 * Finds the first page we are authorized to view.
 * @throws ShowPurchaseOptionsRedirectException when reaching a property/page that requires
 *  displaying purchase options, since there's no valid [MediaPageEntity] in that case.
 */
fun MediaPropertyEntity.getFirstAuthorizedPage(
    currentPage: MediaPageEntity?,
    propertyStore: MediaPropertyStore,
    unauthorizedPageIds: MutableSet<String> = mutableSetOf()
): Flowable<MediaPageEntity> {
    val property = this

    // Convenience function to handle redirects
    fun redirect(redirectPageId: String) = propertyStore.observePage(property, redirectPageId)
        .switchMap { nextPage ->
            // Recursively check the next page
            property.getFirstAuthorizedPage(nextPage, propertyStore, unauthorizedPageIds)
        }

    /**
     * If we are authorized to view this page/property, or redirect behavior isn't configured, returns null.
     */
    fun PermissionSettingsEntity.getRedirectPageId(): String? {
        return alternatePageId?.takeIf { showAlternatePage }
    }

    return if (currentPage == null) {
        // Check property permissions
        val propertyRedirectPage = property.propertyPermissions?.getRedirectPageId()
        if (propertyRedirectPage != null) {
            redirect(propertyRedirectPage)
        } else if (property.propertyPermissions?.showPurchaseOptions == true) {
            Flowable.error(ShowPurchaseOptionsRedirectException(PermissionContext(property.id)))
        } else {
            // We're authorized to view the property, check the main page.
            property.getFirstAuthorizedPage(property.mainPage, propertyStore, unauthorizedPageIds)
        }
    } else if (currentPage.pagePermissions?.showPurchaseOptions == true) {
        Flowable.error(
            ShowPurchaseOptionsRedirectException(PermissionContext(property.id, currentPage.id))
        )
    } else {
        when (val redirectPageId = currentPage.pagePermissions?.getRedirectPageId()) {
            currentPage.id, in unauthorizedPageIds -> {
                // We already checked this page id, or this is a self-reference, so we know
                // we're not authorized to view it.
                Flowable.error(CircularRedirectException())
            }

            null -> {
                // No page to redirect to: we are authorized to render this page.
                Flowable.just(currentPage)
                    .doOnNext { Log.i("Authorized to view page ${currentPage.id}") }
            }

            else -> {
                Log.w("Reached unauthorized page ${currentPage.id}, redirecting to $redirectPageId")
                unauthorizedPageIds += currentPage.id
                redirect(redirectPageId)
            }
        }
    }
}

class CircularRedirectException : IllegalStateException("Circular redirect detected")

class ShowPurchaseOptionsRedirectException(val permissionContext: PermissionContext) :
    RuntimeException("Show purchase options detected")
