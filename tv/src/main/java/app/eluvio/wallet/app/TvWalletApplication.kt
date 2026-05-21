package app.eluvio.wallet.app

import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TvWalletApplication : WalletApplication() {

    @Inject
    lateinit var installReferrerHandler: InstallReferrerHandler

    override fun onCreate() {
        super.onCreate()
        installReferrerHandler.captureInstallReferrer()
    }
}
