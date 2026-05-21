package app.eluvio.wallet.app

import android.content.Context
import android.net.Uri
import app.eluvio.wallet.data.entities.deeplink.DeeplinkRequestEntity
import app.eluvio.wallet.data.stores.DeeplinkStore
import app.eluvio.wallet.screens.home.HomeViewModel
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.rx.unsaved
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Looks for Install Referrer parameters (https://developer.android.com/google/play/installreferrer/library).
 * If found, parses the URL and stores the deeplink request in [DeeplinkStore] to be handled by the app (see [HomeViewModel]).
 */
class InstallReferrerHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deeplinkStore: DeeplinkStore
) {
    fun captureInstallReferrer() {
        if (deeplinkStore.installRefHandled) {
            // Only needs to happen once per app install
            return
        }
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        // Connection established. Check for deeplink parameters.
                        Log.d("Install referrer connection established")
                        handleInstallReferrerResponse(referrerClient.installReferrer)
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                        Log.e("InstallReferrer: feature not supported")
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        // Connection couldn't be established.
                        Log.e("InstallReferrer: service unavailable")
                    }
                }

                // Regardless of result, never invoke InstallReferrerClient again.
                deeplinkStore.installRefHandled = true
                referrerClient.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }

    private fun handleInstallReferrerResponse(response: ReferrerDetails) {
        val installReferrerParams = response.installReferrer
        Log.d("Install installReferrerParams: $installReferrerParams")
        val uriStart = installReferrerParams.indexOf("elvwallet://", ignoreCase = true)
        if (uriStart >= 0) {
            val uri = Uri.parse(installReferrerParams.substring(uriStart))
            Log.d("Install deeplink uri: $uri")
            val action = uri.host ?: return
            if (uri.pathSegments.size < 3) return
            val (marketplace, contract, sku) = uri.pathSegments
            deeplinkStore.setDeeplinkRequest(
                DeeplinkRequestEntity().apply {
                    this.action = action
                    this.marketplace = marketplace
                    this.contract = contract
                    this.sku = sku
                    this.jwt = uri.getQueryParameter("jwt")
                    this.entitlement = uri.getQueryParameter("entitlement")
                    this.backLink = uri.getQueryParameter("back_link")
                }
            ).subscribe().unsaved()
        }
    }
}
