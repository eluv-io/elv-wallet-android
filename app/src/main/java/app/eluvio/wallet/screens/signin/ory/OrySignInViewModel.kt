package app.eluvio.wallet.screens.signin.ory

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.eluvio.wallet.data.UrlShortener
import app.eluvio.wallet.data.entities.v2.LoginProviders
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.data.stores.MetamaskActivationStore
import app.eluvio.wallet.data.stores.TokenStore
import app.eluvio.wallet.network.api.authd.MetamaskActivationData
import app.eluvio.wallet.screens.signin.common.BaseLoginViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class OrySignInViewModel @Inject constructor(
    private val metamaskActivationStore: MetamaskActivationStore,
    propertyStore: MediaPropertyStore,
    tokenStore: TokenStore,
    urlShortener: UrlShortener,
    savedStateHandle: SavedStateHandle
) : BaseLoginViewModel<MetamaskActivationData>(
    propertyStore,
    tokenStore,
    urlShortener,
    LoginProviders.ORY,
    savedStateHandle
) {
    override fun fetchActivationData(): Flowable<MetamaskActivationData> {
        return metamaskActivationStore.observeMetamaskActivationData()
    }

    override fun MetamaskActivationData.checkToken(): Maybe<*> {
        return metamaskActivationStore.checkToken(this)
    }

    override fun MetamaskActivationData.getCode(): String {
        return code
    }

    override fun MetamaskActivationData.getQrUrl(): String {
        return Uri.parse(url)
            .buildUpon()
            .appendQueryParameter("ory", "")
            .apply {
                if (propertyId != null) {
                    appendQueryParameter("pid", propertyId)
                }
            }
            .toString()
    }

    override fun MetamaskActivationData.getPollingInterval(): Duration {
        return 5.seconds
    }
}
