package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.data.entities.v2.PropertySearchFiltersEntity
import app.eluvio.wallet.data.entities.v2.search.FilterValueEntity
import app.eluvio.wallet.data.permissions.PermissionResolver
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.SearchApi
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.network.dto.v2.SearchRequest
import app.eluvio.wallet.util.realm.asFlowable
import app.eluvio.wallet.util.realm.saveAsync
import app.eluvio.wallet.util.realm.saveTo
import app.eluvio.wallet.util.rx.doOnSuccessAsync
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

    fun getFilters(propertyId: String): Flowable<PropertySearchFiltersEntity> {
        return observeRealmAndFetch(
            realmQuery = realm.query<PropertySearchFiltersEntity>(
                "${PropertySearchFiltersEntity::propertyId.name} == $0",
                propertyId
            ).asFlowable(),
            fetchOperation = { _, isFirstState ->
                if (isFirstState) {
                    apiProvider.getApi(SearchApi::class)
                        .flatMap { it.getSearchFilters(propertyId) }
                        .zipWith(apiProvider.getFabricEndpoint())
                        .map { (searchFilters, baseUrl) ->
                            searchFilters.toEntity(propertyId, baseUrl)
                        }
                        .saveTo(realm)
                        .ignoreElement()
                } else null
            })
            .mapNotNull { it.firstOrNull() }
    }

    fun search(
        property: MediaPropertyEntity,
        request: SearchRequest
    ): Single<List<MediaPageSectionEntity>> {
        val sanitizedRequest = request.copy(
            // Any attribute with a tag of "All" should be omitted from the request
            attributes = request.attributes
                ?.filterNot { (_, tags) -> tags.contains(FilterValueEntity.ALL) }
        )
        return apiProvider.getApi(SearchApi::class)
            .flatMap { api ->
                api.search(property.id, sanitizedRequest)
            }
            .zipWith(apiProvider.getFabricEndpoint())
            .map { (response, baseUrl) ->
                response.contents
                    ?.map { section -> section.toEntity(baseUrl) }
                    .orEmpty()
            }
            .doOnSuccessAsync { sections ->
                // Persist only the MediaItems, not the Sections or SectionItems
                val mediaItems = sections.flatMap { it.items }.mapNotNull { it.media }
                realm.saveAsync(mediaItems)
            }
            .doOnSuccess { sections ->
                PermissionResolver.resolvePermissions(
                    sections,
                    property.searchPermissions,
                    property.permissionStates
                )
            }
    }
}
