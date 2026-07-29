package app.eluvio.wallet.data.stores

import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject

/**
 * Provides the categorized rows that make up the redesigned Discover page.
 *
 * FAKE DATA WARNING: the server data model for Discover rows isn't ready yet, so everything
 * except the properties themselves is faked here: real discoverable properties are grouped into
 * hardcoded rows, and the per-property fields the new design needs (hero video, watch progress)
 * are invented. Once the real API lands, only this store should need to change.
 */
class DiscoverRowsStore @Inject constructor(
    private val propertyStore: MediaPropertyStore,
) {
    data class Row(val title: String, val items: List<Item>)

    data class Item(
        val property: MediaPropertyEntity,
        /** FAKE: will come from the server once the data model is ready. */
        val heroVideoUrl: String?,
        /** FAKE: will come from the server once the data model is ready. */
        val hasWatchProgress: Boolean,
    )

    fun observeDiscoverRows(forceRefresh: Boolean = true): Flowable<List<Row>> {
        return propertyStore.observeDiscoverableProperties(forceRefresh)
            .map { properties -> fakeRows(properties) }
    }

    private fun fakeRows(properties: List<MediaPropertyEntity>): List<Row> {
        if (properties.isEmpty()) return emptyList()
        return FAKE_ROW_TITLES.mapIndexed { rowIndex, title ->
            // Rotate the property list per row so every row shows all properties,
            // each in a different order.
            val rotated = properties.rotate(rowIndex * 3)
            Row(
                title = title,
                items = rotated.mapIndexed { index, property ->
                    Item(
                        property = property,
                        // Leave some items without a video to exercise the image fallback.
                        heroVideoUrl = FAKE_HERO_VIDEOS.getOrNull(index % (FAKE_HERO_VIDEOS.size + 1)),
                        hasWatchProgress = Math.floorMod(property.id.hashCode() + rowIndex, 3) != 0,
                    )
                }
            )
        }
    }
}

private fun <T> List<T>.rotate(by: Int): List<T> {
    if (isEmpty()) return this
    val offset = Math.floorMod(by, size)
    return drop(offset) + take(offset)
}

private val FAKE_ROW_TITLES = listOf(
    "Newly Added Sports",
    "Newly Added Entertainment",
    "Trending Now",
    "Popular This Week",
)

private val FAKE_HERO_VIDEOS = listOf(
    "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4",
    "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-25s.mp4",
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
)
