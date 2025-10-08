package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.OwnedPropertiesEntity
import app.eluvio.wallet.data.entities.v2.OwnedPropertiesRealmEntity
import app.eluvio.wallet.data.permissions.PermissionResolver
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.converters.v2.permissions.toPermissionStateEntities
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveAsync
import app.eluvio.wallet.util.realm.saveTo
import app.eluvio.wallet.util.rx.mapNotNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmDictionary
import javax.inject.Inject

class MediaPropertyStore @Inject constructor(
    private val apiProvider: ApiProvider,
    private val realm: Realm,
) {

    fun observeDiscoverableProperties(forceRefresh: Boolean = true): Flowable<List<MediaPropertyEntity>> {
        val orderedProperties =
            Flowable.combineLatest(
                realm.query<MediaPropertyEntity>().asFlowable(),
                realm.query<MediaPropertyEntity.PropertyOrderEntity>().asFlowable()
                    .distinctUntilChanged()
                    .map { list -> list.associateBy { it.propertyId } }
            ) { properties, orderMap ->
                properties.forEach {
                    PermissionResolver.resolvePermissions(
                        it,
                        null,
                        it.permissionStates
                    )
                }
                properties
                    // The local cache of properties can contain properties that didn't come from /mw/properties,
                    // filter out any property the BE didn't explicitly return to show in Discover.
                    .filter { orderMap.containsKey(it.id) }
                    .sortedBy { orderMap[it.id]?.index ?: Int.MAX_VALUE }
            }
                // Because we are observing 2 tables, it's important to not emit the same list twice
                // or we might cancel an ongoing fetch request
                .distinctUntilChanged()

        return observeRealmAndFetch(
            realmQuery = orderedProperties,
            fetchOperation = { _, isFirstState ->
                fetchMediaProperties().takeIf { isFirstState && forceRefresh }
            }
        )
    }

    fun observeOwnedProperties(): Flowable<OwnedPropertiesEntity> {
        return observeRealmAndFetch(
            realmQuery = realm.query<OwnedPropertiesRealmEntity>().asFlowable(),
            fetchOperation = { _, isFirstState ->
                fetchOwnedProperties().takeIf { isFirstState }
            }
        )
            .mapNotNull { it.firstOrNull() }
    }

    private fun fetchOwnedProperties(): Completable {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getProperties(includePublic = false) }
            .map {
                OwnedPropertiesRealmEntity().apply {
                    properties = it.contents.orEmpty()
                        .associate { it.id to it.name }
                        .toRealmDictionary()
                }
            }
            .saveTo(realm)
            .ignoreElement()
    }

    fun fetchMediaProperties(clearOldProperties: Boolean = false): Completable {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getProperties() }
            .doOnError {
                Log.e("Error fetching properties: $it")
            }
            .retry(
                // Would be nice to add a delay between retries, but that's a bit more complex.
                // Maybe some day.
                3
            )
            .zipWith(apiProvider.getFabricEndpoint())
            .flatMapCompletable { (response, baseUrl) ->
                val properties = response.contents.orEmpty()
                    .mapNotNull { propertyDto -> propertyDto.toEntity(baseUrl) }
                val order = properties
                    .mapIndexed { index, property ->
                        MediaPropertyEntity.PropertyOrderEntity().apply {
                            this.propertyId = property.id
                            // This will need to be updated once we support pagination
                            val page = 0
                            this.index = (page * 1000) + index
                        }
                    }

                Completable.mergeArray(
                    realm.saveAsync(properties, clearTable = clearOldProperties),
                    realm.saveAsync(order, clearTable = clearOldProperties)
                )
            }
    }

    fun observePage(
        property: MediaPropertyEntity,
        pageId: String,
        forceRefresh: Boolean = true
    ): Flowable<MediaPageEntity> {
        return observeRealmAndFetch(
            realmQuery = realm.query<MediaPageEntity>(
                "${MediaPageEntity::uid.name} == $0",
                MediaPageEntity.uid(propertyId = property.id, pageId = pageId)
            )
                .asFlowable(),
            fetchOperation = { _, isFirstState ->
                apiProvider.getApi(MediaWalletV2Api::class)
                    .flatMap { api -> api.getPage(property.id, pageId) }
                    .zipWith(apiProvider.getFabricEndpoint())
                    .map { (page, baseUrl) -> page.toEntity(property.id, baseUrl) }
                    .saveTo(realm)
                    .ignoreElement()
                    .takeIf { isFirstState && forceRefresh }
            }
        )
            .mapNotNull { it.firstOrNull() }
            .doOnNext { page ->
                PermissionResolver.resolvePermissions(
                    page,
                    property.resolvedPermissions,
                    property.permissionStates
                )
            }
            .distinctUntilChanged()
    }

    fun observeMediaProperty(
        propertyId: String,
        forceRefresh: Boolean = true
    ): Flowable<MediaPropertyEntity> {
        return observeRealmAndFetch(
            realmQuery = realm.query<MediaPropertyEntity>(
                "${MediaPropertyEntity::id.name} == $0",
                propertyId
            )
                .asFlowable(),
            fetchOperation = { properties, isFirstState ->
                fetchMediaProperty(propertyId)
                    .doOnSubscribe { Log.d("Fetching MediaProperty: $propertyId") }
                    .takeIf { properties.isEmpty() || (isFirstState && forceRefresh) }
            }
        )
            .mapNotNull { it.firstOrNull() }
            .doOnNext { property ->
                PermissionResolver.resolvePermissions(
                    property,
                    null,
                    property.permissionStates
                )
            }
            .distinctUntilChanged()
    }

    /**
     * Observes sections for a given page.
     * By default, it will observe all sections on the page, but you can specify a subset.
     */
    fun observeSections(
        property: MediaPropertyEntity,
        page: MediaPageEntity,
        sectionIds: List<String> = page.sectionIds,
        forceRefresh: Boolean = true
    ): Flowable<List<MediaPageSectionEntity>> {
        return observeRealmAndFetch(
            realmQuery = realm.query<MediaPageSectionEntity>(
                "${MediaPageSectionEntity::id.name} IN $0",
                sectionIds
            ).asFlowable(),
            fetchOperation = { sections, isFirst ->
                val existingSections = if (forceRefresh && isFirst) {
                    emptySet()
                } else {
                    sections.map { it.id }.toSet()
                }
                Log.w("Existing sections (will NOT be fetched): $existingSections")
                fetchMissingSections(property.id, missingSections = sectionIds - existingSections)
            }
        )
            .doOnNext { sections ->
                sections.forEach { section ->
                    PermissionResolver.resolvePermissions(
                        section,
                        page.resolvedPermissions,
                        property.permissionStates
                    )
                }
            }
            .distinctUntilChanged()
    }

    fun fetchMediaProperty(propertyId: String): Completable {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getProperty(propertyId) }
            .doOnError { Log.e("Error fetching property", it) }
            .zipWith(apiProvider.getFabricEndpoint())
            .mapNotNull { (response, baseUrl) -> response.toEntity(baseUrl) }
            .saveTo(realm)
            .ignoreElement()
    }

    private fun fetchMissingSections(
        propertyId: String,
        missingSections: List<String>
    ): Completable {
        return if (missingSections.isEmpty()) {
            Completable.complete()
                .doOnSubscribe {
                    Log.d("All sections are already in the database. Nothing to fetch.")
                }
        } else {
            apiProvider.getApi(MediaWalletV2Api::class)
                .doOnSubscribe { Log.d("Fetching missing sections: $missingSections") }
                .flatMap { api -> api.getSectionsById(propertyId, missingSections.toList()) }
                .doOnError { Log.e("Error fetching sections", it) }
                .zipWith(apiProvider.getFabricEndpoint())
                .map { (response, baseUrl) ->
                    response.contents.orEmpty().map { sectionDto -> sectionDto.toEntity(baseUrl) }
                }
                .saveTo(realm, clearTable = false)
                .ignoreElement()
        }
    }

    /**
     * Fetches permission states for a property and updates the property in the database.
     */
    fun fetchPermissionStates(propertyId: String): Completable {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getPermissionStates(propertyId) }
            .map { response -> response.toPermissionStateEntities() }
            .doOnSuccess { permissionStates ->
                // Find the property in the database and update it
                realm.writeBlocking {
                    query<MediaPropertyEntity>("${MediaPropertyEntity::id.name} == $0", propertyId)
                        .first().find()?.permissionStates = permissionStates
                }
            }
            .ignoreElement()
    }
}
