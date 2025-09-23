package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.JsonMapConverter.toJsonObject
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.arrayOfObjectsProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.stringProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations

/**
 * Tool handler for getting all root scopes.
 *
 * This tool retrieves all scopes that have no parent (root scopes).
 */
class ScopesRootsToolHandler(private val responseFormatter: ResponseFormatterService = ResponseFormatterService()) : ToolHandler {

    override val name: String = "scopes.roots"

    override val description: String = "Get all root scopes (scopes without parents)"

    override val annotations: ToolAnnotations? = Annotations.readOnlyIdempotent()

    override val input: Tool.Input = toolInput(required = emptyList()) { }

    override val output: Tool.Output = toolOutput(required = listOf("roots")) {
        arrayOfObjectsProperty("roots", itemRequired = listOf("canonicalAlias", "title")) {
            stringProperty("canonicalAlias")
            stringProperty("title")
            stringProperty("description")
        }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        ctx.services.logger.debug("Getting root scopes")

        val result = ctx.ports.query.getRootScopes(GetRootScopesQuery())

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val responseMap = responseFormatter.formatRootScopesForMcp(result.value)
                val json = responseMap.toJsonObject()
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}
