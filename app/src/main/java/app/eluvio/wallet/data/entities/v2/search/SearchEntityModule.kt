package app.eluvio.wallet.data.entities.v2.search

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

@Module
@InstallIn(SingletonComponent::class)
object SearchEntityModule {
    @Provides
    @ElementsIntoSet
    fun provideEntity(): Set<KClass<out TypedRealmObject>> =
        setOf(
            FilterAttributeEntity::class,
            FilterValueEntity::class,
            PrimaryFilterOptionsEntity::class,
            SecondaryFilterOptionsEntity::class
        )
}
