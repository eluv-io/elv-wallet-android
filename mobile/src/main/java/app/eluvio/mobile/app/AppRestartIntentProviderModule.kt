package app.eluvio.mobile.app

import android.content.Context
import android.content.Intent
import app.eluvio.wallet.app.AppRestartIntentProvider
import app.eluvio.mobile.MainMobileActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppRestartIntentProviderModule {
    @Provides
    @Singleton
    fun provideAppRestartIntentProvider(
        @ApplicationContext context: Context,
    ): AppRestartIntentProvider = AppRestartIntentProvider {
        Intent(context, MainMobileActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
