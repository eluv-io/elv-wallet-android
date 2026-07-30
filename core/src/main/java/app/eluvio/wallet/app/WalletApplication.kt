package app.eluvio.wallet.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.coroutineScope
import app.eluvio.wallet.di.TokenAwareHttpClient
import app.eluvio.wallet.util.coil.ContentFabricSizingInterceptor
import app.eluvio.wallet.util.coil.FabricImageInterceptor
import app.eluvio.wallet.util.coil.FabricUrlKeyer
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.google.firebase.FirebaseApp
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

/**
 * Shared Application for both :tv and :mobile. Handles cross-cutting concerns: RxJava error
 * handling, lifecycle-driven Realm migration + fabric-config refresh, Timber/Crashlytics
 * tree planting, and Coil's image-loader factory wired to the token-aware OkHttp client.
 *
 * Subclasses must add @HiltAndroidApp.
 */
abstract class WalletApplication : Application(), SingletonImageLoader.Factory {
    @Inject
    lateinit var fabricConfigRefresher: FabricConfigRefresher

    @Inject
    @TokenAwareHttpClient
    lateinit var httpClient: OkHttpClient

    @Inject
    lateinit var migrationManager: MigrationManager

    @Inject
    lateinit var appInfo: AppInfo

    override fun onCreate() {
        super.onCreate()

        // Consume all errors without crashing.
        RxJavaPlugins.setErrorHandler {
            Timber.e(it)
        }

        ProcessLifecycleOwner.get().lifecycle.apply {
            coroutineScope.launch { migrationManager.applyMigration() }
            addObserver(fabricConfigRefresher)
        }

        if (appInfo.isDebug) {
            Timber.plant(Timber.DebugTree())
        } else if (FirebaseApp.getApps(this).isNotEmpty()) {
            // No FirebaseApp means the google-services plugin wasn't applied (no
            // google-services.json at build time). Touching Crashlytics in that state throws.
            Timber.plant(CrashlyticsTree())
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Coil checks if Application implements SingletonImageLoader.Factory and calls this
        // automatically. We provide our own OkHttpClient so image requests include fabric
        // token headers.
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { httpClient }))
                add(FabricUrlKeyer())
                add(SvgDecoder.Factory())
                add(FabricImageInterceptor())
                add(ContentFabricSizingInterceptor())
            }
            .build()
    }
}
