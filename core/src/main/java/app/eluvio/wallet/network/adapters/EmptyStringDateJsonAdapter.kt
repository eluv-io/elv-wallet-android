package app.eluvio.wallet.network.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Server sometimes sends "" instead of 'null' for empty values.
 * This adapter converts empty strings to null and delegates anything else to [delegate].
 */
class EmptyStringAsNullJsonAdapter<T>(private val delegate: JsonAdapter<T>) : JsonAdapter<T>() {

    override fun fromJson(reader: JsonReader): T? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull<T>()
        } else {
            val value = reader.peekJson().nextString()
            return if (value == "") {
                // Consume "" from actual reader
                reader.nextString()
                null
            } else {
                delegate.fromJson(reader)
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: T?) {
        delegate.toJson(writer, value)
    }
}

/**
 * Returns a JSON adapter equal to this JSON adapter,
 * but with support for reading empty strings as nulls.
 */
fun <T> JsonAdapter<T>.emptyStringAsNull(): JsonAdapter<T> {
    return if (this is EmptyStringAsNullJsonAdapter) {
        this
    } else {
        EmptyStringAsNullJsonAdapter(this)
    }
}
