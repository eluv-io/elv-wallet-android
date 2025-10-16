package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.dto.v2.MediaPropertyDto
import app.eluvio.wallet.testing.RealmTestRule
import app.eluvio.wallet.testing.TestLogRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import io.realm.kotlin.ext.query
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaPropertyStoreTest {
    @get:Rule
    val testLogRule = TestLogRule()

    @get:Rule
    val realmRule = RealmTestRule()
    private val realm get() = realmRule.realm

    private val api: MediaWalletV2Api = mockk {
        every { this@mockk.getProperty(any()) } returns Single.just(mockk())
    }
    private val apiProvider: ApiProvider = mockk {
        every { getApi(MediaWalletV2Api::class) } returns Single.just(api)
        every { getFabricEndpoint() } returns Single.just("http://fabric.url")
    }

    private val store by lazy { MediaPropertyStore(apiProvider, realm) }

    @Test
    fun `fetchMediaProperty fetches from api and saves to realm`() {
        // GIVEN
        val propertyId = "iprop123"
        val propertyDto = mockk<MediaPropertyDto>(relaxed = true) {
            every { id } returns propertyId
            every { name } returns "foo"
        }
        every { api.getProperty(propertyId) } returns Single.just(propertyDto)
        assertNull(realm.query<MediaPropertyEntity>().first().find())

        // WHEN
        store.fetchMediaProperty(propertyId).test().await()

        // THEN
        verify { api.getProperty(propertyId) }
        assertEquals(realm.query<MediaPropertyEntity>().first().find()?.id, propertyId)
    }

    @Test
    fun `observeMediaProperty when db is empty should fetch property`() = runTest {
        // GIVEN
        val propertyId = "prop123"

        // WHEN
        val testObserver = store.observeMediaProperty(propertyId, forceRefresh = false).test()
        testObserver.awaitCount(1)

        // THEN
        verify { api.getProperty(propertyId) }
    }

    @Test
    fun `observeMediaProperty when db has data and forceRefresh is false should NOT fetch`() {
        // GIVEN
        val propertyId = "iprop123"
        val propertyEntity = MediaPropertyEntity().apply { id = propertyId }
        realm.writeBlocking { copyToRealm(propertyEntity) }

        // WHEN
        store.observeMediaProperty(propertyId, forceRefresh = false).test().awaitCount(1)

        // THEN
        verify(exactly = 0) { api.getProperty(propertyId) }
    }

    @Test
    fun `observeMediaProperty when db has data and forceRefresh is true should fetch`() {
        // GIVEN
        val propertyId = "iprop123"
        val propertyEntity = MediaPropertyEntity().apply { id = propertyId }
        realm.writeBlocking { copyToRealm(propertyEntity) }

        // WHEN
        store.observeMediaProperty(propertyId, forceRefresh = true).test().awaitCount(2)

        // THEN
        verify(exactly = 1) { api.getProperty(propertyId) }
    }
}
