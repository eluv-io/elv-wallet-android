package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.SearchFiltersEntity
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.SearchApi
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.network.dto.v2.SearchRequest
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveTo
import app.eluvio.wallet.util.rx.mapNotNull
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject

class PropertySearchStore @Inject constructor(
    private val apiProvider: ApiProvider,
    private val realm: Realm,
) {

    fun getFilters(propertyId: String): Flowable<SearchFiltersEntity> {
        return observeRealmAndFetch(
            realmQuery = realm.query<SearchFiltersEntity>(
                "${SearchFiltersEntity::propertyId.name} == $0",
                propertyId
            ).asFlowable(),
            fetchOperation = { _, isFirstState ->
                if (isFirstState) {
                    apiProvider.getApi(SearchApi::class)
                        .flatMap { it.getSearchFilters(propertyId) }
                        .map { it.toEntity(propertyId) }
                        .saveTo(realm)
                        .ignoreElement()
                } else null
            })
            .mapNotNull { it.firstOrNull() }
    }

    fun search(propertyId: String, query: String): Single<List<MediaPageSectionEntity>> {
        return apiProvider.getApi(SearchApi::class)
            .flatMap {
                it.search(
                    propertyId,
                    SearchRequest(search_term = query)
                )
            }
            .zipWith(apiProvider.getFabricEndpoint())
            .map { (response, baseUrl) ->
                response.contents
                    ?.map { section -> section.toEntity(baseUrl) }
                    .orEmpty()
            }
    }
}
