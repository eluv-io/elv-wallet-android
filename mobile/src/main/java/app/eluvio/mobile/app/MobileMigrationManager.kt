package app.eluvio.mobile.app

import android.content.Context
import app.eluvio.wallet.app.AppInfo
import app.eluvio.wallet.app.MigrationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Mobile has no migrations yet — :mobile was introduced after every breaking shape change in
 * the auth/token format. Inherits the base class's "record current versionCode" behavior so
 * future migrations have a comparison point.
 */
class MobileMigrationManager @Inject constructor(
    @ApplicationContext context: Context,
    appInfo: AppInfo,
) : MigrationManager(context, appInfo)

@Module
@InstallIn(SingletonComponent::class)
interface MobileMigrationManagerModule {
    @Binds
    fun bind(impl: MobileMigrationManager): MigrationManager
}
