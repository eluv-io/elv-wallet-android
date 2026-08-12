package app.eluvio.mobile.app

import android.content.Context
import app.eluvio.wallet.app.MigrationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Mobile has no migrations yet — :mobile was introduced after every breaking shape change in
 * the auth/token format.
 */
class MobileMigrationManager @Inject constructor(
    @ApplicationContext context: Context,
) : MigrationManager(context)

@Module
@InstallIn(SingletonComponent::class)
interface MobileMigrationManagerModule {
    @Binds
    fun bind(impl: MobileMigrationManager): MigrationManager
}
