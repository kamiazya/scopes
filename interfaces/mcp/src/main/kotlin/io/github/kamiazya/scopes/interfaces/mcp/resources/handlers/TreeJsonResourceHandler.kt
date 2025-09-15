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
                asJson = true
            )
        }

        val rootResult = ports.query.getScopeByAlias(GetScopeByAliasQuery(pureAlias))
        
        return when (rootResult) {
            is Either.Left -> {
                val error = rootResult.value
                ResourceHelpers.createErrorResourceResult(
                    uri = uri,
                    code = -32011,
                    message = error.toString(),
                    errorType = error::class.simpleName,
                    asJson = true
                )
            }
            is Either.Right -> {
                val scope = rootResult.value
                createTreeJsonResult(scope, depthValue, ports, services)
            }
        }
    }

    private suspend fun createTreeJsonResult(
        scope: ScopeResult, 
        depthValue: Int, 
        ports: Ports, 
        services: Services
    ): ReadResourceResult {
        var nodeCount = 0
        val maxNodes = 1000
        var latestUpdatedAt = scope.updatedAt

        suspend fun buildScopeNode(alias: String, currentDepth: Int): JsonObject? {
            if (nodeCount >= maxNodes) return null
            nodeCount++

            val nodeResult = ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
            return nodeResult.fold(
                { _ ->
                    buildJsonObject {
                        put("canonicalAlias", alias)
                        put("title", alias)
                    }
                },
                { s ->
                    if (s.updatedAt > latestUpdatedAt) {
                        latestUpdatedAt = s.updatedAt
                    }

                    val childrenJson = if (currentDepth < depthValue && nodeCount < maxNodes) {
                        val childrenResult = ports.query.getChildren(GetChildrenQuery(parentId = s.id))
                        childrenResult.fold(
                            { emptyList<JsonObject>() },
                            { ch -> 
                                ch.scopes.mapNotNull { c -> 
                                    buildScopeNode(c.canonicalAlias, currentDepth + 1) 
                                } 
                            }
                        )
                    } else {
                        emptyList()
                    }

                    buildJsonObject {
                        put("canonicalAlias", s.canonicalAlias)
                        put("title", s.title)
                        s.description?.let { put("description", it) }
                        put("updatedAt", s.updatedAt.toString())
                        putJsonArray("children") { childrenJson.forEach { add(it) } }
                        putJsonArray("links") {
                            add(
                                buildJsonObject {
                                    put("rel", "self")
                                    put("uri", "scopes:/scope/${s.canonicalAlias}")
                                }
                            )
                        }
                    }
                }
            )
        }

        val json = buildScopeNode(scope.canonicalAlias, 1)
        val jsonText = json?.toString() ?: buildJsonObject {
            put("error", "Tree too large (>$maxNodes nodes)")
            put("canonicalAlias", scope.canonicalAlias)
            put("title", scope.title)
        }.toString()

        val etag = ResourceHelpers.computeEtag(jsonText)
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = jsonText,
                    uri = "scopes:/tree/${scope.canonicalAlias}?depth=$depthValue",
                    mimeType = mimeType
                )
            ),
            _meta = buildJsonObject {
                put("etag", etag)
                put("lastModified", latestUpdatedAt.toString())
                put("nodeCount", nodeCount)
                put("maxDepth", depthValue)
                putJsonArray("links") {
                    add(
                        buildJsonObject {
                            put("rel", "self")
                            put("uri", "scopes:/tree/${scope.canonicalAlias}?depth=$depthValue")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("rel", "scope")
                            put("uri", "scopes:/scope/${scope.canonicalAlias}")
                        }
                    )
                }
            }
        )
    }
}