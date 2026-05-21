package app.eluvio.wallet.app

import app.eluvio.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {
    @Provides
    @Singleton
    fun provideAppInfo(): AppInfo = AppInfo(
        applicationId = BuildConfig.APPLICATION_ID,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        isDebug = BuildConfig.DEBUG,
    )
}
