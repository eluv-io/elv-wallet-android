package app.eluvio.wallet.data.permissions

import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.data.stores.MediaPropertyStore
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject

/**
 * Resolves a [PermissionContext] into a [PermissionContext.Resolved] by observing the hierarchy
 * defined by the context. The resolution order is as follows:
 * Property -> Page -> Section -> SectionItem -> MediaItem
 * Resolution will stop at the first null value, except for MediaItems, which we will try to
 * pull directly, even when no Section/SectionItem is defined.
 */
class PermissionContextResolver @Inject constructor(
    private val propertyStore: MediaPropertyStore,
    private val contentStore: ContentStore,
) {

    fun resolve(permissionContext: PermissionContext): Flowable<PermissionContext.Resolved> {
        val (propertyId, pageId, sectionId, sectionItemId, mediaItemId) = permissionContext
        return propertyStore.observeMediaProperty(propertyId, forceRefresh = false)
            .map { PermissionContext.Resolved(property = it) }
            .switchMap { observePage(pageId, context = it) }
            .switchMap { observeSection(sectionId, context = it) }
            .map { resolved ->
                val sectionItem = resolved.section?.items?.firstOrNull { it.id == sectionItemId }
                resolved.copy(sectionItem = sectionItem)
            }
            .switchMap { observeMediaItem(mediaItemId, context = it) }
            .distinctUntilChanged()
    }

    private fun observeMediaItem(
        mediaItemId: String?,
        context: PermissionContext.Resolved
    ): Flowable<PermissionContext.Resolved> {
        return when {
            mediaItemId == null -> {
                // No mediaItem requested. Return as-is.
                Flowable.just(context)
            }

            context.sectionItem != null -> {
                // sectionItem already exists, assume mediaItem is part of it.
                Flowable.just(context.copy(mediaItem = context.sectionItem.media?.takeIf { it.id == mediaItemId }))
            }

            else -> {
                // mediaItem requested, but no SectionItem. Observe it separately and find closest parent
                return contentStore.observeMediaItem(mediaItemId)
                    .doOnNext {
                        // Find the closes parent
                        val parentPermissions = with(context) {
                            (sectionItem ?: section ?: page ?: property).resolvedPermissions
                        }
                        PermissionResolver.resolvePermissions(
                            it,
                            parentPermissions,
                            context.property.permissionStates
                        )
                    }
                    .map { context.copy(mediaItem = it) }
            }
        }
    }

    private fun observePage(
        pageId: String?,
        context: PermissionContext.Resolved
    ): Flowable<PermissionContext.Resolved> {
        val mainPage = context.property.mainPage
        return when (pageId) {
            // No page. Stop resolution.
            null -> Flowable.just(context)

            mainPage?.id -> {
                // Short circuit. If we are interested the main page, we already have it
                Flowable.just(context.copy(page = mainPage))
            }

            else -> propertyStore.observePage(context.property, pageId, forceRefresh = false)
                .map { context.copy(page = it) }
        }
    }

    private fun observeSection(
        sectionId: String?,
        context: PermissionContext.Resolved
    ): Flowable<PermissionContext.Resolved> {
        return if (context.page == null || sectionId == null) {
            Flowable.just(context)
        } else {
            propertyStore.observeSections(
                context.property,
                context.page,
                sectionIds = listOf(sectionId),
                forceRefresh = false
            )
                .map { context.copy(section = it.firstOrNull()) }
        }
    }
}
