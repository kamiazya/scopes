package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.idempotencyKeyProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Tool handler for updating existing scopes.
 *
 * This tool updates a scope's title and/or description, with idempotency support.
 */
class ScopeUpdateToolHandler : ToolHandler {

    override val name: String = "scopes.update"

    override val description: String = "Update an existing scope's title or description"

    override val annotations: ToolAnnotations? = Annotations.destructiveNonIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("alias")) {
        putJsonObject("alias") {
            put("type", "string")
            put("minLength", 1)
            put("description", "Scope alias to update")
        }
        putJsonObject("title") {
            put("type", "string")
            put("minLength", 1)
            put("description", "New title (optional)")
        }
        putJsonObject("description") {
            put("type", "string")
            put("description", "New description (optional)")
        }
        idempotencyKeyProperty()
    }

    override val output: Tool.Output = toolOutput(required = listOf("canonicalAlias", "title", "updatedAt")) {
        putJsonObject("canonicalAlias") { put("type", "string") }
        putJsonObject("title") { put("type", "string") }
        putJsonObject("description") { put("type", "string") }
        putJsonObject("updatedAt") { put("type", "string") }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        val title = ctx.services.codec.getString(ctx.args, "title")
        val description = ctx.services.codec.getString(ctx.args, "description")
        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Updating scope: $alias")

        return ctx.services.idempotency.getOrCompute(name, ctx.args, idempotencyKey) {
            // First get the scope to update
            val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
            val (scopeId, canonicalAlias) = when (scopeResult) {
                is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(scopeResult.value)
                is Either.Right -> scopeResult.value.id to scopeResult.value.canonicalAlias
            }

            // Update the scope
            val result = ctx.ports.command.updateScope(
                UpdateScopeCommand(
                    id = scopeId,
                    title = title,
                    description = description,
                ),
            )

            when (result) {
                is Either.Left -> ctx.services.errors.mapContractError(result.value)
                is Either.Right -> {
                    val updated = result.value
                    val json = buildJsonObject {
                        put("canonicalAlias", canonicalAlias)
                        put("title", updated.title)
                        updated.description?.let { put("description", it) }
                        put("updatedAt", updated.updatedAt.toString())
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
