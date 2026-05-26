package app.eluvio.wallet.screens.gallery

import androidx.compose.runtime.Immutable
import app.eluvio.wallet.app.BaseViewModel
import app.eluvio.wallet.data.entities.MediaEntity
import app.eluvio.wallet.data.stores.ContentStore
import app.eluvio.wallet.di.ApiProvider
import com.stavfx.nav3hiltvm.annotations.HiltNavKeyViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy

@HiltNavKeyViewModel
open class ImageGalleryViewModel(
    @NavArg private val navArgs: ImageGalleryNavArgs,
    private val contentStore: ContentStore,
    private val apiProvider: ApiProvider,
) : BaseViewModel<ImageGalleryViewModel.State>(State()) {
    @Immutable
    data class State(val images: List<GalleryImage> = emptyList()) {
        data class GalleryImage(val url: String, val name: String?, val aspectRatio: Float? = null)
    }

    override fun onResume() {
        super.onResume()
        apiProvider.getFabricEndpoint()
            .flatMapPublisher { endpoint ->
                contentStore.observeMediaItem(navArgs.mediaEntityId)
                    .map { media -> media to endpoint }
            }
            .map { (media, endpoint) ->
                when (media.mediaType) {
                    MediaEntity.MEDIA_TYPE_GALLERY -> {
                        media.gallery.mapNotNull { galleryItem ->
                            galleryItem.imagePath?.let { path ->
                                State.GalleryImage(
                                    "$endpoint$path",
                                    galleryItem.name,
                                    galleryItem.imageAspectRatio
                                )
                            }
                        }
                    }

                    MediaEntity.MEDIA_TYPE_IMAGE -> {
                        val image = media.mediaFile.takeIf { it.isNotEmpty() }
                            ?.let { path ->
                                State.GalleryImage(
                                    "$endpoint$path",
                                    media.name,
                                    media.imageAspectRatio
                                )
                            }
                        listOfNotNull(image)
                    }

                    else -> emptyList()
                }
            }
            .subscribeBy(
                onNext = {
                    //todo throw if list is empty?
                    updateState { State(images = it) }
                },
                onError = {

                }
            )
            .addTo(disposables)
    }
}
