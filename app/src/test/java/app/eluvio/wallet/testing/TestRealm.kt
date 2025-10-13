package app.eluvio.wallet.testing

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KClass

/**
 * Returns all entities we want to consider.
 */
private val allEntityClasses: List<KClass<out TypedRealmObject>>
    get() = Reflections(
        ConfigurationBuilder()
            .forPackage("app.eluvio.wallet")
            .addUrls(ClasspathHelper.forJavaClassPath())
    ).let {
        it.getSubTypesOf(RealmObject::class.java) +
                it.getSubTypesOf(EmbeddedRealmObject::class.java)
    }
        .map { it.kotlin }


fun testRealm(): Realm =
    RealmConfiguration.Builder(allEntityClasses.toSet())
        .name("test-realm")
        .inMemory()
        .build().let { Realm.open(it) }
