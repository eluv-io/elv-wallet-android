package app.eluvio.wallet.testing

import io.github.xilinjia.krdb.Realm
import io.github.xilinjia.krdb.RealmConfiguration
import io.github.xilinjia.krdb.types.EmbeddedRealmObject
import io.github.xilinjia.krdb.types.RealmObject
import io.github.xilinjia.krdb.types.TypedRealmObject
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

class RealmTestRule : TestWatcher() {
    companion object {
        /**
         * Returns all entities we want to consider.
         */
        val allEntityClasses: List<Class<out TypedRealmObject>>
            get() = Reflections(
                ConfigurationBuilder()
                    .forPackage("app.eluvio.wallet")
                    .addUrls(ClasspathHelper.forJavaClassPath())
            ).let {
                it.getSubTypesOf(RealmObject::class.java) +
                        it.getSubTypesOf(EmbeddedRealmObject::class.java)
            }
                .sortedBy { it.name }
    }

    lateinit var realm: Realm

    private val config = RealmConfiguration.Builder(
        allEntityClasses.map { it.kotlin }.toSet()
    )
        .inMemory()
        .directory("./build/tmp")
        .name("test-realm")
        .build()

    override fun starting(description: Description?) {
        realm = Realm.open(config)
    }

    override fun finished(description: Description?) {
        realm.close()
    }
}
