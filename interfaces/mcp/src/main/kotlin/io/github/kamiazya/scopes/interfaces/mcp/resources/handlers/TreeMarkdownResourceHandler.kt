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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Resource handler for scope tree in Markdown format.
 * 
 * Provides scope hierarchy with immediate children rendered as Markdown (depth=1).
 */
class TreeMarkdownResourceHandler : ResourceHandler {
    
    override val uriPattern: String = "scopes:/tree.md/{canonicalAlias}"
    
    override val name: String = "Scope Tree (Markdown)"
    
    override val description: String = "Scope with immediate children rendered as Markdown (depth=1)"
    
    override val mimeType: String = "text/markdown"
    
    override suspend fun read(req: ReadResourceRequest, ports: Ports, services: Services): ReadResourceResult {
        val uri = req.uri
        val prefix = "scopes:/tree.md/"
        val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

        services.logger.debug("Reading tree Markdown for alias: $alias")

        if (alias.isBlank()) {
            return ResourceHelpers.createSimpleTextResult(
                uri = uri,
                text = "Invalid resource: missing alias",
                mimeType = mimeType
            )
        }

        val rootResult = ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
        
        return when (rootResult) {
            is Either.Left -> {
                val error = rootResult.value
                ResourceHelpers.createSimpleTextResult(
                    uri = uri,
                    text = "Error: ${error}",
                    mimeType = mimeType
                )
            }
            is Either.Right -> {
                val scope = rootResult.value
                createTreeMarkdownResult(uri, scope, ports, services)
            }
        }
    }

    private suspend fun createTreeMarkdownResult(
        uri: String,
        scope: ScopeResult,
        ports: Ports,
        services: Services
    ): ReadResourceResult {
        val childrenResult = ports.query.getChildren(GetChildrenQuery(parentId = scope.id))

        var latestUpdated = scope.updatedAt
        childrenResult.fold(
            { /* no children to check */ },
            { children ->
                children.scopes.forEach { child ->
                    if (child.updatedAt > latestUpdated) {
                        latestUpdated = child.updatedAt
                    }
                }
            }
        )

        val md = buildString {
            appendLine("# ${scope.title} (${scope.canonicalAlias})")
            scope.description?.let { appendLine("\n$it") }
            appendLine("\n## Children")
            childrenResult.fold(
                { _ -> appendLine("(no children or failed to load)") },
                { children ->
                    if (children.scopes.isEmpty()) {
                        appendLine("(no children)")
                    } else {
                        children.scopes.forEach { ch ->
                            appendLine("- ${ch.title} (${ch.canonicalAlias})" + (ch.description?.let { ": $it" } ?: ""))
                        }
                    }
                }
            )
            appendLine("\n[JSON] scopes:/tree/${scope.canonicalAlias}")
        }

        val etag = ResourceHelpers.computeEtag(md)
        return ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = md,
                    uri = uri,
                    mimeType = mimeType
                )
            ),
            _meta = buildJsonObject {
                put("etag", etag)
                put("lastModified", latestUpdated.toString())
                putJsonArray("links") {
                    add(
                        buildJsonObject {
                            put("rel", "self")
                            put("uri", "scopes:/tree.md/${scope.canonicalAlias}")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("rel", "scope")
                            put("uri", "scopes:/scope/${scope.canonicalAlias}")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("rel", "tree")
                            put("uri", "scopes:/tree/${scope.canonicalAlias}")
                        }
                    )
                }
            }
        )
    }
}