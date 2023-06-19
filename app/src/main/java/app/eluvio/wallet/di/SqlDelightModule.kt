package app.eluvio.wallet.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.eluvio.wallet.WalletDatabase
import app.eluvio.wallet.sqldelight.NftQueries
import app.eluvio.wallet.sqldelight.UserQueries
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SqlDelightModule {

    @Provides
    @Singleton
    fun provideDatabase(driver: SqlDriver): WalletDatabase {
        return WalletDatabase(driver)
    }

    @Provides
    fun provideDriver(@ApplicationContext context: Context): SqlDriver {
        return AndroidSqliteDriver(WalletDatabase.Schema, context, "media-wallet.db")
    }
}

/**
 * A module that provides specific query interfaces.
 * Yes, you could directly depend [WalletDatabase] and use whatever queries you need from it,
 * but this way stores/repos are more declarative about their actual dependencies.
 */
@InstallIn(SingletonComponent::class)
@Module
object QueriesModule {
    @Provides
    fun provideUserQueries(db: WalletDatabase): UserQueries = db.userQueries

    @Provides
    fun providerNftQueries(db: WalletDatabase): NftQueries = db.nftQueries
}
