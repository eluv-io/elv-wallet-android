package app.eluvio.wallet.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import app.eluvio.wallet.data.GridContentOverride
import app.eluvio.wallet.data.PropertyLink
import app.eluvio.wallet.data.permissions.PermissionContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * NavType registry for the 4 routes whose fields contain types Nav 2.8 can't auto-resolve
 * (collections of @Serializable types; the @Serializable sealed [NavTarget]).
 *
 * Single-value @Serializable fields (e.g. [PermissionContext], [GridContentOverride])
 * also need an explicit NavType for Nav 2.8 — see comment in `*NavArgs.kt`.
 */

val PropertyLinkListNavType: NavType<ArrayList<PropertyLink>> =
    object : NavType<ArrayList<PropertyLink>>(isNullableAllowed = false) {
        private val serializer = ListSerializer(PropertyLink.serializer())

        override fun put(bundle: Bundle, key: String, value: ArrayList<PropertyLink>) {
            bundle.putString(key, Json.encodeToString(serializer, value.toList()))
        }

        override fun get(bundle: Bundle, key: String): ArrayList<PropertyLink>? {
            @Suppress("DEPRECATION")
            val raw = bundle.getString(key) ?: return null
            return ArrayList(Json.decodeFromString(serializer, raw))
        }

        override fun parseValue(value: String): ArrayList<PropertyLink> =
            ArrayList(Json.decodeFromString(serializer, value))

        override fun serializeAsValue(value: ArrayList<PropertyLink>): String =
            Uri.encode(Json.encodeToString(serializer, value.toList()))
    }

val PermissionContextNavType: NavType<PermissionContext> = serializableNavType()
val GridContentOverrideNullableNavType: NavType<GridContentOverride?> =
    serializableNavType(nullable = true)
val NavTargetNullableNavType: NavType<NavTarget?> =
    serializableNavType(nullable = true)

/**
 * typeMap entries to register per-route with `composable<T>(typeMap = ...)`.
 */
val PropertyDetailTypeMap: Map<KType, NavType<*>> = mapOf(
    typeOf<ArrayList<PropertyLink>>() to PropertyLinkListNavType,
)
val MediaGridTypeMap: Map<KType, NavType<*>> = mapOf(
    typeOf<PermissionContext>() to PermissionContextNavType,
    typeOf<GridContentOverride?>() to GridContentOverrideNullableNavType,
)
val PurchasePromptTypeMap: Map<KType, NavType<*>> = mapOf(
    typeOf<PermissionContext>() to PermissionContextNavType,
)
val SignInTypeMap: Map<KType, NavType<*>> = mapOf(
    typeOf<NavTarget?>() to NavTargetNullableNavType,
)
