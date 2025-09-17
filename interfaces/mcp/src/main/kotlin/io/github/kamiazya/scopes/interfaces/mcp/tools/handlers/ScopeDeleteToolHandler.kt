package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for deleting scopes.
 *
 * This tool deletes a scope that must have no children, with idempotency support.
 */
class ScopeDeleteToolHandler : ToolHandler {

    override val name: String = "scopes.delete"

    override val description: String = "Delete a scope (must have no children)"

    override val annotations: ToolAnnotations? = ToolAnnotations(
        title = null,
        readOnlyHint = false,
        destructiveHint = true,
        idempotentHint = false,
    )

    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                add("alias")
            }
            putJsonObject("properties") {
                putJsonObject("alias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Scope alias to delete")
                }
                putJsonObject("idempotencyKey") {
                    put("type", "string")
                    put("pattern", IdempotencyService.IDEMPOTENCY_KEY_PATTERN_STRING)
                    put("description", "Idempotency key to prevent duplicate operations")
                }
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("canonicalAlias") { put("type", "string") }
                putJsonObject("deleted") { put("type", "boolean") }
            }
            putJsonArray("required") {
                add("canonicalAlias")
                add("deleted")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Deleting scope: $alias")

        return ctx.services.idempotency.getOrCompute(name, ctx.args, idempotencyKey) {
            // First get the scope to delete
            val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
            val (scopeId, canonicalAlias) = when (scopeResult) {
                is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(scopeResult.value)
                is Either.Right -> scopeResult.value.id to scopeResult.value.canonicalAlias
            }

            // Delete the scope
            val result = ctx.ports.command.deleteScope(DeleteScopeCommand(id = scopeId))

            when (result) {
                is Either.Left -> ctx.services.errors.mapContractError(result.value)
                is Either.Right -> {
                    val json = buildJsonObject {
                        put("canonicalAlias", canonicalAlias)
                        put("deleted", true)
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
