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
    private val tokenStore: TokenStore,
    private val signOutHandler: SignOutHandler,
) : MigrationManager(context) {

    override suspend fun applyMigration() {
        runOnce("force_reauth_for_sessions_without_refresh_token") {
            // Sessions minted by builds that predate refresh tokens can't recover from an
            // expired token - AccessTokenInterceptor gives up and signs them out on the next 401
            // regardless. Do it up front instead of stranding the user mid-session.
            // Checking for the token itself rather than the app version means only the sessions
            // that are actually broken get signed out.
            if (tokenStore.isLoggedIn && tokenStore.refreshToken.get() == null) {
                signOutHandler.signOut(completeMessage = null, restartAppOnComplete = true).await()
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface TvMigrationManagerModule {
    @Binds
    fun bind(impl: TvMigrationManager): MigrationManager
}
