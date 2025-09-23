package io.github.kamiazya.scopes.interfaces.mcp.support

import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8

/**
 * Helper functions for MCP resource handling.
 */
object ResourceHelpers {

    /**
     * Compute ETag for resource content using SHA-256.
     * This provides better collision resistance than the previous simple hash.
     */
    fun computeEtag(text: String): String = text.encodeUtf8().sha256().hex()

    /**
     * Parse tree alias to extract pure alias and depth parameter.
     * Example: "my-alias?depth=3" -> ("my-alias", 3)
     */
    fun parseTreeAlias(alias: String): Pair<String, Int> {
        val qPos = alias.indexOf('?')
        val pureAlias = if (qPos >= 0) alias.substring(0, qPos) else alias
        val depth = if (qPos >= 0) {
            alias.substring(qPos + 1).split('=').let {
                if (it.size == 2 && it[0] == "depth") it[1].toIntOrNull() else null
            }
        } else {
            null
        }
        return pureAlias to (depth ?: 1).coerceIn(1, 5)
    }

    /** Overload that allows maxDepth to be specified by caller. */
    fun parseTreeAlias(alias: String, maxDepth: Int): Pair<String, Int> {
        val (pure, d) = parseTreeAlias(alias)
        return pure to d.coerceIn(1, maxDepth)
    }

    /**
     * Extract canonical alias from a resource URI based on a fixed prefix.
     * Returns empty string when the prefix does not match.
     */
    fun extractAlias(uri: String, prefix: String): String = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

    /**
     * Create an error resource result with proper formatting.
     */
    fun createErrorResourceResult(uri: String, code: Int, message: String, errorType: String? = null, asJson: Boolean = false): ReadResourceResult {
        val payload = if (asJson) {
            buildJsonObject {
                put("code", code)
                putJsonObject("data") {
                    put("type", errorType ?: "Error")
                    put("message", message)
                }
            }.toString()
        } else {
            buildJsonObject {
                put(
                    "error",
                    buildJsonObject {
                        put("code", code)
                        put("message", message)
                    },
                )
            }.toString()
        }

        val etag = computeEtag(payload)
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = payload,
                    uri = uri,
                    mimeType = "application/json",
                ),
            ),
            _meta = buildJsonObject {
                put("etag", etag)
                put("lastModified", Clock.System.now().toString())
            },
        )
    }

    /**
     * Create a simple text resource result.
     * Note: Per MCP spec, text/markdown and text/plain resources should not include _meta.
     */
    fun createSimpleTextResult(uri: String, text: String, mimeType: String): ReadResourceResult = if (mimeType == "text/markdown" || mimeType == "text/plain") {
        // No metadata for text/markdown and text/plain
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = text,
                    uri = uri,
                    mimeType = mimeType,
                ),
            ),
        )
    } else {
        // Include metadata for other MIME types
        val etag = computeEtag(text)
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = text,
                    uri = uri,
                    mimeType = mimeType,
                ),
            ),
            _meta = buildJsonObject {
                put("etag", etag)
                put("lastModified", Clock.System.now().toString())
            },
        )
    }

    /**
     * Create scope details resource result with proper links.
     */
    fun createScopeDetailsResult(uri: String, scope: ScopeResult): ReadResourceResult {
        val payload = buildJsonObject {
            put("canonicalAlias", scope.canonicalAlias)
            put("title", scope.title)
            scope.description?.let { put("description", it) }
            put("createdAt", scope.createdAt.toString())
            put("updatedAt", scope.updatedAt.toString())
            putJsonArray("links") { scopeLinks(scope.canonicalAlias).forEach { add(it) } }
        }.toString()

        val etag = computeEtag(payload)
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = payload,
                    uri = uri,
                    mimeType = "application/json",
                ),
            ),
            _meta = buildJsonObject {
                put("etag", etag)
                put("lastModified", scope.updatedAt.toString())
                putJsonArray("links") { scopeLinks(scope.canonicalAlias).forEach { add(it) } }
            },
        )
    }

    /**
     * Build a single link object { rel, uri }.
     */
    fun link(rel: String, uri: String): JsonObject = buildJsonObject {
        put("rel", rel)
        put("uri", uri)
    }

    /**
     * Standard links for a scope resource.
     */
    fun scopeLinks(canonicalAlias: String): List<JsonObject> = listOf(
        link("self", "scopes:/scope/$canonicalAlias"),
        link("tree", "scopes:/tree/$canonicalAlias"),
        link("tree.md", "scopes:/tree.md/$canonicalAlias"),
    )

    /**
     * Standard links for a tree resource.
     */
    fun treeLinks(canonicalAlias: String, depth: Int): List<JsonObject> = listOf(
        link("self", "scopes:/tree/$canonicalAlias?depth=$depth"),
        link("scope", "scopes:/scope/$canonicalAlias"),
    )
}
