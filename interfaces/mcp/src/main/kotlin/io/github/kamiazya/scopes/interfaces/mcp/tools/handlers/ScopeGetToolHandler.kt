package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.JsonMapConverter.toJsonObject
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.aliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for getting a scope by alias.
 *
 * This tool retrieves detailed information about a scope given its alias.
 */
class ScopeGetToolHandler(private val responseFormatter: ResponseFormatterService = ResponseFormatterService()) : ToolHandler {

    override val name: String = "scopes.get"

    override val description: String = "Get a scope by alias (exact match)"

    override val input: Tool.Input = toolInput(required = listOf("alias")) {
        aliasProperty(description = "Scope alias to look up")
    }

    override val annotations: ToolAnnotations? = Annotations.readOnlyIdempotent()

    override val output: Tool.Output = toolOutput(required = listOf("canonicalAlias", "title", "createdAt", "updatedAt")) {
        putJsonObject("canonicalAlias") { put("type", "string") }
        putJsonObject("title") { put("type", "string") }
        putJsonObject("description") { put("type", "string") }
        putJsonObject("createdAt") { put("type", "string") }
        putJsonObject("updatedAt") { put("type", "string") }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        ctx.services.logger.debug("Getting scope by alias: $alias")

        val result = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val responseMap = responseFormatter.formatScopeForMcp(result.value)
                CallToolResult(content = listOf(TextContent(responseMap.toJsonObject().toString())))
            }
        }
    }
}
