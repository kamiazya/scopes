package io.github.kamiazya.scopes.interfaces.mcp.support

import kotlinx.serialization.json.*

/**
 * Default implementation of ArgumentCodec for tool argument handling.
 *
 * This class is internal as it should only be used within the MCP module.
 * External modules should depend on the ArgumentCodec interface.
 */
internal class DefaultArgumentCodec : ArgumentCodec {

    override fun canonicalizeArguments(arguments: Map<String, JsonElement>): String {
        // Remove nulls, sort keys at root, recursively canonicalize
        val filtered = buildJsonObject {
            arguments
                .filterValues { it !is JsonNull }
                .toSortedMap()
                .forEach { (k, v) -> put(k, v) }
        }
        return canonicalizeJson(filtered)
    }

    override fun canonicalizeJson(json: JsonElement): String = when (json) {
        is JsonNull -> "null"
        is JsonPrimitive -> if (json.isString) {
            buildString {
                append('"')
                append(escapeJsonString(json.content))
                append('"')
            }
        } else {
            json.toString()
        }
        is JsonArray -> json.joinToString(prefix = "[", postfix = "]") { canonicalizeJson(it) }
        is JsonObject -> {
            val entries = json.entries.sortedBy { it.key }
            entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                val key = buildString {
                    append('"')
                    append(escapeJsonString(k))
                    append('"')
                }
                "$key:${canonicalizeJson(v)}"
            }
        }
    }

    private fun escapeJsonString(str: String): String = buildString {
        for (char in str) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                in '\u0000'..'\u001F' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                else -> append(char)
            }
        }
    }

    override fun buildCacheKey(toolName: String, arguments: Map<String, JsonElement>, idempotencyKey: String?): String {
        val canonical = canonicalizeArguments(arguments)
        // Simple hash function for KMP compatibility
        var hash = 0L
        for (char in canonical) {
            hash = ((hash shl 5) - hash) + char.code
            hash = hash and 0xFFFFFFFFL // Keep it 32-bit
        }
        val argsHash = hash.toString(16).padStart(8, '0')
        val effectiveKey = idempotencyKey ?: "auto"
        return "$toolName|$effectiveKey|$argsHash"
    }

    override fun getString(args: Map<String, JsonElement>, key: String, required: Boolean): String? {
        val element = args[key]
        return when {
            element == null -> {
                require(!required) { "Missing required parameter: $key" }
                null
            }
            element is JsonPrimitive && element.isString -> element.content
            else -> {
                require(!required) { "Parameter '$key' must be a string" }
                null
            }
        }
    }

    override fun getBoolean(args: Map<String, JsonElement>, key: String, default: Boolean): Boolean {
        val element = args[key] ?: return default
        return when {
            element is JsonPrimitive && !element.isString -> {
                element.content.toBooleanStrictOrNull() ?: default
            }
            else -> default
        }
    }
}
