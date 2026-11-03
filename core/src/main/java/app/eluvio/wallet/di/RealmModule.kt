package app.eluvio.wallet.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.github.xilinjia.krdb.Realm
import io.github.xilinjia.krdb.RealmConfiguration
import io.github.xilinjia.krdb.types.TypedRealmObject
import kotlin.reflect.KClass

@InstallIn(SingletonComponent::class)
@Module
object RealmModule {
    @Provides
    fun provideRealm(realmObjects: Set<@JvmSuppressWildcards KClass<out TypedRealmObject>>): Realm {
        val config = RealmConfiguration.Builder(realmObjects)
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        return Realm.open(config)
    }

    @Provides
    @ElementsIntoSet
    fun provideRealmObjects(): Set<KClass<out TypedRealmObject>> = emptySet()
}
