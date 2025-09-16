package io.github.kamiazya.scopes.interfaces.mcp.support

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Stores idempotency results with TTL.
 */
private data class StoredResult(val result: CallToolResult, val timestamp: Instant)

/**
 * Default implementation of IdempotencyService using thread-safe in-memory storage.
 *
 * This implementation uses a Mutex to ensure thread-safety when accessing the cache.
 * This class is internal as it should only be used within the MCP module.
 */
internal class DefaultIdempotencyService(
    private val argumentCodec: ArgumentCodec,
    private val ttl: Duration = 10.minutes,
    private val maxEntries: Int = 10_000,
) : IdempotencyService {

    private val idempotencyStore = mutableMapOf<String, StoredResult>()
    private val mutex = Mutex()

    companion object {
        private val IDEMPOTENCY_KEY_PATTERN = Regex("^[A-Za-z0-9_-]{8,128}$")

        private fun isValidKey(key: String): Boolean = key.matches(IDEMPOTENCY_KEY_PATTERN)
    }

    override suspend fun checkIdempotency(toolName: String, arguments: Map<String, JsonElement>, idempotencyKey: String?): CallToolResult? {
        val effectiveKey = idempotencyKey ?: return null

        if (!isValidKey(effectiveKey)) {
            return CallToolResult(
                content = listOf(TextContent(text = "Invalid idempotency key format")),
                isError = true,
                _meta = buildJsonObject {
                    put("code", -32602)
                    put("type", "InvalidIdempotencyKey")
                    put("key", effectiveKey)
                    put("pattern", IDEMPOTENCY_KEY_PATTERN.pattern)
                },
            )
        }

        val cacheKey = argumentCodec.buildCacheKey(toolName, arguments, effectiveKey)

        return mutex.withLock {
            // Clean up expired entries periodically
            cleanupExpiredEntriesInternal()

            val stored = idempotencyStore[cacheKey]

            if (stored != null) {
                val now = Clock.System.now()
                val age = now - stored.timestamp

                if (age < ttl) {
                    // Return cached result
                    stored.result
                } else {
                    // Expired, remove it
                    idempotencyStore.remove(cacheKey)
                    null
                }
            } else {
                null
            }
        }
    }

    override suspend fun storeIdempotency(toolName: String, arguments: Map<String, JsonElement>, result: CallToolResult, idempotencyKey: String?) {
        val effectiveKey = idempotencyKey ?: return

        // Validate the idempotency key using the same logic as checkIdempotency
        if (!isValidKey(effectiveKey)) {
            // Invalid key - return early without storing anything
            return
        }

        val cacheKey = argumentCodec.buildCacheKey(toolName, arguments, effectiveKey)
        val stored = StoredResult(result, Clock.System.now())

        mutex.withLock {
            idempotencyStore[cacheKey] = stored
            // Clean up old entries (simple TTL-based cleanup)
            cleanupExpiredEntriesInternal()
        }
    }

    override suspend fun cleanupExpiredEntries() {
        mutex.withLock {
            cleanupExpiredEntriesInternal()
        }
    }

    /**
     * Internal cleanup method that must be called within a mutex lock.
     */
    private fun cleanupExpiredEntriesInternal() {
        val now = Clock.System.now()
        val cutoff = now - ttl

        // Remove expired entries
        idempotencyStore.entries.removeIf { entry ->
            entry.value.timestamp < cutoff
        }

        // If still over limit, remove oldest entries
        if (idempotencyStore.size > maxEntries) {
            val entriesToRemove = idempotencyStore.size - maxEntries
            idempotencyStore.entries
                .sortedBy { it.value.timestamp }
                .take(entriesToRemove)
                .forEach { idempotencyStore.remove(it.key) }
        }
    }
}
