package app.eluvio.wallet.screens.videoplayer

import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.permissions.PermissionResolver
import app.eluvio.wallet.data.stores.MediaPropertyStore
import app.eluvio.wallet.di.ApiProvider
import app.eluvio.wallet.network.api.mwv2.MediaWalletV2Api
import app.eluvio.wallet.network.converters.v2.toEntity
import app.eluvio.wallet.network.dto.v2.AutoplayRequest
import app.eluvio.wallet.util.logging.Log
import app.eluvio.wallet.util.realm.saveTo
import app.eluvio.wallet.util.rx.mapNotNull
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.zipWith
import io.realm.kotlin.Realm
import javax.inject.Inject

/**
 * Answers "the user just finished this item - what plays next?".
 *
 * The server owns the ordering and the eligibility rules, but says nothing about entitlement: the
 * run is handed over regardless of permission status, so deciding which of those items to actually
 * surface is our job.
 */
class UpNextLoader @Inject constructor(
    private val apiProvider: ApiProvider,
    private val propertyStore: MediaPropertyStore,
    private val realm: Realm,
) {
    /**
     * The next item to play after [mediaItemId], or empty when there's nothing worth offering.
     * Nothing to play arrives as an empty response rather than an error, so an empty response and
     * a failed one both come back as empty.
     *
     * @param sectionId the section being viewed, which resolves groups configured as
     *   "<Current Section>".
     * @param mediaListId the media list being viewed, if the user came in through one.
     */
    fun getNextItem(
        propertyId: String,
        mediaItemId: String,
        sectionId: String?,
        mediaListId: String?,
    ): Maybe<MediaEntity> {
        return apiProvider.getApi(MediaWalletV2Api::class)
            .flatMap { api ->
                api.getAutoplayNext(
                    propertyId,
                    AutoplayRequest(
                        mediaId = mediaItemId,
                        sectionId = sectionId,
                        mediaListId = mediaListId,
                    )
                )
            }
            .zipWith(apiProvider.getFabricEndpoint())
            .map { (response, baseUrl) ->
                response.contents.orEmpty().map { it.toEntity(baseUrl) }
            }
            .saveTo(realm)
            .zipWith(
                propertyStore.observeMediaProperty(propertyId, forceRefresh = false).firstOrError()
            )
            .doOnSuccess { (candidates, property) ->
                Log.d("Up next returned ${candidates.size} candidates after $mediaItemId")
                // Returned items carry their permissions unresolved, and [firstPresentable] reads them.
                PermissionResolver.resolvePermissions(
                    candidates,
                    property.resolvedPermissions,
                    property.permissionStates
                )
            }
            .mapNotNull { (candidates, _) -> firstPresentable(candidates) }
            .doOnError { error ->
                Log.w("Up next lookup failed for $mediaItemId", error)
            }
            .onErrorComplete()
    }

    /**
     * The first candidate worth putting in front of the viewer.
     *
     * Hidden items are content they aren't meant to see, and disabled ones can be neither played
     * nor bought, so both are passed over. An item behind a purchase gate is kept: we have a gate
     * to show for it, and the run shouldn't silently skip a paid episode.
     */
    private fun firstPresentable(candidates: List<MediaEntity>): MediaEntity? {
        return candidates.firstOrNull { !it.isHidden && !it.isDisabled }
    }
}
