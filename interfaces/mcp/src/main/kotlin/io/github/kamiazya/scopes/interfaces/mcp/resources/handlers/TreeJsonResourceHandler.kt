package io.github.kamiazya.scopes.interfaces.mcp.resources.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.resources.ResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.support.ResourceHelpers
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*

/**
 * Resource handler for scope tree in JSON format.
 *
 * Provides scope hierarchy with configurable depth in JSON format.
 */
class TreeJsonResourceHandler : ResourceHandler {

    override val uriPattern: String = "scopes:/tree/{canonicalAlias}"

    override val name: String = "Scope Tree (JSON)"

    override val description: String = "Scope with children (configurable depth)."

    override val mimeType: String = "application/json"

    override suspend fun read(req: ReadResourceRequest, ports: Ports, services: Services): ReadResourceResult {
        val uri = req.uri
        val prefix = "scopes:/tree/"
        val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

        val (pureAlias, depthValue) = ResourceHelpers.parseTreeAlias(alias)

        services.logger.debug("Reading tree JSON for alias: $pureAlias with depth: $depthValue")

        if (pureAlias.isBlank()) {
            return ResourceHelpers.createErrorResourceResult(
                uri = "scopes:/tree/$pureAlias?depth=$depthValue",
                code = -32602,
                message = "Missing or invalid alias in resource URI. Optional ?depth=1..5 supported.",
                asJson = true,
            )
        }

        val rootResult = ports.query.getScopeByAlias(GetScopeByAliasQuery(pureAlias))

        return when (rootResult) {
            is Either.Left -> services.errors.mapContractErrorToResource(uri, rootResult.value)
            is Either.Right -> {
                val scope = rootResult.value
                createTreeJsonResult(scope, depthValue, ports, services)
            }
        }
    }

    private suspend fun createTreeJsonResult(scope: ScopeResult, depthValue: Int, ports: Ports, services: Services): ReadResourceResult {
        val builder = TreeNodeBuilder(ports, services, depthValue)
        val json = builder.buildScopeNode(scope.canonicalAlias, 1)

        val jsonText = json?.toString() ?: buildJsonObject {
            put("error", "Tree too large (>${builder.maxNodes} nodes)")
            put("canonicalAlias", scope.canonicalAlias)
            put("title", scope.title)
        }.toString()

        val etag = ResourceHelpers.computeEtag(jsonText)
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = jsonText,
                    uri = "scopes:/tree/${scope.canonicalAlias}?depth=$depthValue",
                    mimeType = mimeType,
                ),
            ),
            _meta = buildTreeMetadata(scope, depthValue, etag, builder.latestUpdatedAt, builder.nodeCount),
        )
    }

    private fun buildTreeMetadata(scope: ScopeResult, depthValue: Int, etag: String, latestUpdatedAt: Instant, nodeCount: Int): JsonObject = buildJsonObject {
        put("etag", etag)
        put("lastModified", latestUpdatedAt.toString())
        put("nodeCount", nodeCount)
        put("maxDepth", depthValue)
        putJsonArray("links") {
            add(
                buildJsonObject {
                    put("rel", "self")
                    put("uri", "scopes:/tree/${scope.canonicalAlias}?depth=$depthValue")
                },
            )
            add(
                buildJsonObject {
                    put("rel", "scope")
                    put("uri", "scopes:/scope/${scope.canonicalAlias}")
                },
            )
        }
    }

    private class TreeNodeBuilder(private val ports: Ports, private val services: Services, private val maxDepth: Int) {
        var nodeCount = 0
        val maxNodes = 1000
        var latestUpdatedAt: Instant = Instant.DISTANT_PAST

        suspend fun buildScopeNode(alias: String, currentDepth: Int): JsonObject? {
            currentCoroutineContext().ensureActive()

            if (nodeCount >= maxNodes) return null
            nodeCount++

            val nodeResult = ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
            return nodeResult.fold(
                { _ -> buildErrorNode(alias) },
                { scope -> buildSuccessNode(scope, currentDepth) },
            )
        }

        private fun buildErrorNode(alias: String): JsonObject = buildJsonObject {
            put("canonicalAlias", alias)
            put("title", alias)
        }

        private suspend fun buildSuccessNode(scope: ScopeResult, currentDepth: Int): JsonObject {
            updateLatestTimestamp(scope.updatedAt)
            val children = if (shouldFetchChildren(currentDepth)) fetchChildren(scope, currentDepth) else emptyList()

            return buildJsonObject {
                put("canonicalAlias", scope.canonicalAlias)
                put("title", scope.title)
                scope.description?.let { put("description", it) }
                put("updatedAt", scope.updatedAt.toString())
                putJsonArray("children") { children.forEach { add(it) } }
                putJsonArray("links") {
                    add(
                        buildJsonObject {
                            put("rel", "self")
                            put("uri", "scopes:/scope/${scope.canonicalAlias}")
                        },
                    )
                }
            }
        }

        private fun updateLatestTimestamp(updatedAt: Instant) {
            if (updatedAt > latestUpdatedAt) {
                latestUpdatedAt = updatedAt
            }
        }

        private fun shouldFetchChildren(currentDepth: Int): Boolean = currentDepth < maxDepth && nodeCount < maxNodes

        private suspend fun fetchChildren(scope: ScopeResult, currentDepth: Int): List<JsonObject> {
            val childrenResult = ports.query.getChildren(GetChildrenQuery(parentId = scope.id))
            return childrenResult.fold(
                { emptyList() },
                { ch -> ch.scopes.mapNotNull { c -> buildChildNode(c, currentDepth) } },
            )
        }

        private suspend fun buildChildNode(child: ScopeResult, currentDepth: Int): JsonObject? {
            if (!currentCoroutineContext().isActive) return null
            return buildScopeNode(child.canonicalAlias, currentDepth + 1)
        }
    }
}
