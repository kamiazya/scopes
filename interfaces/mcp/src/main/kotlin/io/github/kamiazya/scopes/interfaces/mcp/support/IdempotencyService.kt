package io.github.kamiazya.scopes.interfaces.mcp.support

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import kotlinx.serialization.json.JsonElement

/**
 * Service for handling tool call idempotency.
 *
 * This service ensures that duplicate tool calls with the same
 * arguments return cached results instead of being re-executed.
 */
interface IdempotencyService {
    /**
     * Check if a tool call with the given arguments has been seen before.
     *
     * @param toolName The name of the tool being called
     * @param arguments The tool arguments
     * @param idempotencyKey Optional explicit idempotency key
     * @return Cached result if found, null if this is a new call
     */
    suspend fun checkIdempotency(toolName: String, arguments: Map<String, JsonElement>, idempotencyKey: String? = null): CallToolResult?

    /**
     * Store the result of a tool call for future idempotency checks.
     *
     * @param toolName The name of the tool that was called
     * @param arguments The tool arguments
     * @param result The result to store
     * @param idempotencyKey Optional explicit idempotency key
     */
    suspend fun storeIdempotency(toolName: String, arguments: Map<String, JsonElement>, result: CallToolResult, idempotencyKey: String? = null)

    /**
     * Clean up expired idempotency entries.
     * This should be called periodically to prevent memory leaks.
     */
    suspend fun cleanupExpiredEntries()

    /**
     * Get cached result or compute and store new result.
     *
     * This is a convenience method that combines checkIdempotency and storeIdempotency.
     *
     * @param toolName The name of the tool being called
     * @param arguments The tool arguments
     * @param idempotencyKey Optional explicit idempotency key
     * @param compute Function to compute the result if not cached
     * @return The cached or computed result
     */
    suspend fun getOrCompute(
        toolName: String,
        arguments: Map<String, JsonElement>,
        idempotencyKey: String? = null,
        compute: suspend () -> CallToolResult,
    ): CallToolResult {
        // Check for cached result first
        val cached = checkIdempotency(toolName, arguments, idempotencyKey)
        if (cached != null) {
            return cached
        }

        // Compute new result
        val result = compute()

        // Store for future calls
        storeIdempotency(toolName, arguments, result, idempotencyKey)

        return result
    }
}
