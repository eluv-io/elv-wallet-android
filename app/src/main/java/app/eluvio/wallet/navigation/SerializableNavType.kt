package app.eluvio.wallet.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * JSON-encoded [NavType] for any `@Serializable` type. Used to register complex route fields
 * (collections, sealed types, nested data classes) with Jetpack Navigation 2.8's `typeMap`.
 *
 * The encoded form is a URI-safe JSON string. `parseValue` receives a URI-decoded string
 * from Navigation, so we don't need to decode it again here.
 */
inline fun <reified T> serializableNavType(
    nullable: Boolean = false,
    serializer: KSerializer<T> = Json.serializersModule.serializer(),
): NavType<T> = object : NavType<T>(isNullableAllowed = nullable) {
    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, Json.encodeToString(serializer, value))
    }

    override fun get(bundle: Bundle, key: String): T? {
        @Suppress("DEPRECATION")
        val raw = bundle.getString(key) ?: return null
        return Json.decodeFromString(serializer, raw)
    }

    override fun parseValue(value: String): T = Json.decodeFromString(serializer, value)

    override fun serializeAsValue(value: T): String =
        Uri.encode(Json.encodeToString(serializer, value))
}
