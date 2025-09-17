package io.github.kamiazya.scopes.interfaces.mcp.support

import kotlinx.serialization.json.*

/**
 * Utility object for safely converting Map structures to JSON.
 * Handles nested Maps, Lists, and primitive types properly.
 */
object JsonMapConverter {
    /**
     * Convert a Map to a JsonObject, recursively handling nested structures.
     */
    fun Map<*, *>.toJsonObject(): JsonObject = buildJsonObject {
        forEach { (k, v) ->
            val key = k.toString()
            put(key, v.toJsonElement())
        }
    }

    /**
     * Convert any value to a JsonElement, handling all common types.
     */
    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Map<*, *> -> toJsonObject()
        is List<*> -> buildJsonArray { forEach { add(it.toJsonElement()) } }
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }
}
