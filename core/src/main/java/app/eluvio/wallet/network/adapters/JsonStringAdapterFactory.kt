package app.eluvio.wallet.network.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

/**
 * A [JsonQualifier] that tells [Moshi] to unwrap a string before parsing it.
 * This is useful for when you have a string that is actually a json object.
 */
@JsonQualifier
annotation class JsonString

/**
 * A [JsonAdapter.Factory] that unwraps a string before parsing it.
 * This factory will apply to anything annotated with [JsonString]
 */
object JsonStringAdapterFactory : JsonAdapter.Factory {

    private class UnwrappingAdapter<T>(
        private val type: Type,
        private val moshi: Moshi
    ) : JsonAdapter<T>() {
        override fun fromJson(reader: JsonReader): T? {
            val string = reader.nextString()                // Unwrap the string
            return moshi.adapter<T>(type).fromJson(string)  // Parse it as JSON
        }

        override fun toJson(writer: JsonWriter, value: T?) {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Returns a [JsonAdapter] for `type` annotated with [JsonString], or null if `annotations` doesn't contain
     * [JsonString].
     */
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (annotations.none { it is JsonString }) return null
        return UnwrappingAdapter<Any>(type, moshi)
    }
}
