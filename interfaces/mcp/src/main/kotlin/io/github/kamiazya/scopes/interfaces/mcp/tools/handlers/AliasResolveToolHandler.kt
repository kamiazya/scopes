package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.aliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.stringProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Tool handler for resolving aliases to scopes.
 *
 * This tool resolves a scope by alias (exact match only) and returns
 * basic scope information including the canonical alias.
 */
class AliasResolveToolHandler : ToolHandler {

    override val name: String = "aliases.resolve"

    override val description: String = "Resolve a scope by alias (exact match only). Returns the canonical alias if found."

    override val annotations: ToolAnnotations? = Annotations.readOnlyIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("alias")) {
        aliasProperty(description = "Alias to resolve (exact match only)")
    }

    override val output: Tool.Output = toolOutput(required = listOf("alias", "canonicalAlias", "title")) {
        stringProperty("alias", description = "The input alias provided by the user")
        stringProperty("canonicalAlias", description = "The canonical (normalized) alias of the scope")
        stringProperty("title", description = "The title of the resolved scope")
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        ctx.services.logger.debug("Resolving alias: $alias")

        val result = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val scope = result.value
                val json = buildJsonObject {
                    put("alias", alias) // Input alias
                    put("canonicalAlias", scope.canonicalAlias) // Normalized alias
                    put("title", scope.title)
                }
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}
