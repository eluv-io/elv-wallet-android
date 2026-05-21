package app.eluvio.wallet.app

import android.content.Context
import app.eluvio.wallet.data.SignOutHandler
import app.eluvio.wallet.data.stores.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.rx3.await
import javax.inject.Inject

class TvMigrationManager @Inject constructor(
    @ApplicationContext context: Context,
    appInfo: AppInfo,
    private val tokenStore: TokenStore,
    private val signOutHandler: SignOutHandler,
) : MigrationManager(context, appInfo) {

    override suspend fun runMigrations(lastVersionCode: Int) {
        if (lastVersionCode in 1..33 && tokenStore.isLoggedIn) {
            // User was on a build that had no refresh tokens; force re-auth so their session
            // picks up the new token shape. One-shot — once they sign back in lastVersionCode
            // moves above 33 and this never fires again.
            signOutHandler.signOut(completeMessage = null, restartAppOnComplete = true).await()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface TvMigrationManagerModule {
    @Binds
    fun bind(impl: TvMigrationManager): MigrationManager
}
