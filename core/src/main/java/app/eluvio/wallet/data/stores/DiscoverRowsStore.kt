package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.DiscoverRowEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.permissions.PermissionResolver
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.network.dto.v2.DiscoverRowDto
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveAsync
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmList
import javax.inject.Inject

/**
 * Provides the categorized rows of Properties that make up the Discover page.
 */
class DiscoverRowsStore @Inject constructor(
    private val apiProvider: ApiProvider,
    private val realm: Realm,
) {
    data class Row(val title: String, val properties: List<MediaPropertyEntity>)

    fun observeDiscoverRows(forceRefresh: Boolean = true): Flowable<List<Row>> {
        // We observe the whole Properties table and pick out the ones the rows reference,
        // instead of driving an "id IN $0" query off the rows. That query would skip
        // [asFlowable]'s copyFromRealm for every Property that Discover doesn't show, but it
        // costs nesting the two queries in a switchMap, and today the two sets are nearly
        // identical - i.e. every Property shows up in at least one Discover row. Worth revisiting
        // if that gap ever grows.
        val rows = Flowable.combineLatest(
            realm.query<DiscoverRowEntity>().asFlowable(),
            realm.query<MediaPropertyEntity>().asFlowable()
        ) { rows, properties ->
            val propertiesById = properties.associateBy { it.id }
            rows.sortedBy { it.index }
                .map { row ->
                    Row(
                        title = row.title,
                        // A row can reference properties we don't have (yet), skip those.
                        properties = row.propertyIds.mapNotNull { propertiesById[it] }
                    )
                }
                .filter { it.properties.isNotEmpty() }
        }
            // Resolving permissions isn't free, so only do it for Properties that actually
            // made it into a row. The same Property can appear in more than one row.
            // This has to stay ahead of [distinctUntilChanged]: resolving mutates the very
            // entities it compares, which would make it see a change on every emission.
            .doOnNext { discoverRows ->
                discoverRows.flatMap { it.properties }
                    .distinctBy { it.id }
                    .forEach {
                        PermissionResolver.resolvePermissions(it, null, it.permissionStates)
                    }
            }
            // Because we are observing 2 tables, it's important to not emit the same list twice
            // or we might cancel an ongoing fetch request
            .distinctUntilChanged()

        return observeRealmAndFetch(
            realmQuery = rows,
            fetchOperation = { _, isFirstState ->
                fetchDiscoverRows().takeIf { isFirstState && forceRefresh }
            }
        )
    }

    private fun fetchDiscoverRows(): Completable {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api -> api.getDiscover() }
            .doOnError { Log.e("Error fetching discover rows: $it") }
            .retry(3)
            .zipWith(apiProvider.getFabricEndpoint())
            .flatMapCompletable { (response, baseUrl) ->
                val properties = response.properties.orEmpty().values
                    .mapNotNull { propertyDto -> propertyDto.toEntity(baseUrl) }
                val rows = response.contents.orEmpty()
                    .filter { it.type == DiscoverRowDto.TYPE_PROPERTIES }
                    .mapIndexed { index, rowDto ->
                        DiscoverRowEntity().apply {
                            this.index = index
                            title = rowDto.title.orEmpty()
                            propertyIds = rowDto.propertyIds.orEmpty().toRealmList()
                        }
                    }

                Completable.mergeArray(
                    // Properties are also fetched (and cached) elsewhere, so don't clear the
                    // table - just update the ones Discover knows about.
                    realm.saveAsync(properties),
                    realm.saveAsync(rows, clearTable = true)
                )
            }
    }
}
