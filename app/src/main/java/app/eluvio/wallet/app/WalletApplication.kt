package app.eluvio.wallet.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import app.eluvio.wallet.BuildConfig
import app.eluvio.wallet.di.TokenAwareHttpClient
import app.eluvio.wallet.util.coil.ContentFabricSizingInterceptor
import app.eluvio.wallet.util.coil.FabricUrlMapper
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WalletApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var fabricConfigRefresher: FabricConfigRefresher

    @Inject
    @TokenAwareHttpClient
    lateinit var httpClient: OkHttpClient

    @Inject
    lateinit var installReferrerHandler: InstallReferrerHandler

    @Inject
    lateinit var migrationManager: MigrationManager

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.apply {
            coroutineScope.launch { migrationManager.applyMigration() }
            addObserver(fabricConfigRefresher)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        installReferrerHandler.captureInstallReferrer()
    }

    override fun newImageLoader(): ImageLoader {
        // Coil checks if Application implements ImageLoaderFactory and calls this automatically.
        // We provide our own OkHttpClient so image requests include fabric token headers.
        return ImageLoader.Builder(this).okHttpClient(httpClient)
            .components {
                add(SvgDecoder.Factory())
                add(ContentFabricSizingInterceptor())
                add(FabricUrlMapper())
            }
            .build()
    }
}
