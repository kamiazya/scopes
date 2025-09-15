package io.github.kamiazya.scopes.interfaces.mcp.support

import kotlinx.serialization.json.JsonElement

/**
 * Service for encoding, decoding, and canonicalizing tool arguments.
 *
 * This service provides utilities for working with tool arguments,
 * including normalization for caching and validation.
 */
interface ArgumentCodec {
    /**
     * Canonicalize arguments for consistent caching and comparison.
     *
     * This ensures that equivalent argument sets (even with different
     * ordering or formatting) produce the same canonical representation.
     *
     * @param arguments The arguments to canonicalize
     * @return A canonical string representation
     */
    fun canonicalizeArguments(arguments: Map<String, JsonElement>): String

    /**
     * Canonicalize a single JSON element.
     *
     * @param json The JSON element to canonicalize
     * @return A canonical string representation
     */
    fun canonicalizeJson(json: JsonElement): String

    /**
     * Build a cache key for the given tool and arguments.
     *
     * @param toolName The tool name
     * @param arguments The tool arguments
     * @param idempotencyKey Optional explicit idempotency key
     * @return A cache key string
     */
    fun buildCacheKey(toolName: String, arguments: Map<String, JsonElement>, idempotencyKey: String? = null): String

    /**
     * Extract a string argument, with optional required validation.
     *
     * @param args The argument map
     * @param key The argument key
     * @param required Whether the argument is required
     * @return The string value, or null if not present and not required
     * @throws IllegalArgumentException if required but missing
     */
    fun getString(args: Map<String, JsonElement>, key: String, required: Boolean = false): String?

    /**
     * Extract a boolean argument.
     *
     * @param args The argument map
     * @param key The argument key
     * @param default Default value if not present
     * @return The boolean value or default
     */
    fun getBoolean(args: Map<String, JsonElement>, key: String, default: Boolean = false): Boolean
}
