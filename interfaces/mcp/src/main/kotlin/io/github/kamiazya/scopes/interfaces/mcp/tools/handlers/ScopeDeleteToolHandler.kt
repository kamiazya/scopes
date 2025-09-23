package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.getScopeByAliasOrFail
import io.github.kamiazya.scopes.interfaces.mcp.support.idempotencyKeyProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.withIdempotency
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Tool handler for deleting scopes.
 *
 * This tool deletes a scope that must have no children, with idempotency support.
 */
class ScopeDeleteToolHandler : ToolHandler {

    override val name: String = "scopes.delete"

    override val description: String = "Delete a scope (must have no children)"

    override val annotations: ToolAnnotations? = Annotations.destructiveNonIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("alias")) {
        putJsonObject("alias") {
            put("type", "string")
            put("minLength", 1)
            put("description", "Scope alias to delete")
        }
        idempotencyKeyProperty()
    }

    override val output: Tool.Output = toolOutput(required = listOf("canonicalAlias", "deleted")) {
        putJsonObject("canonicalAlias") { put("type", "string") }
        putJsonObject("deleted") { put("type", "boolean") }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Deleting scope: $alias")

        return ctx.withIdempotency(name, idempotencyKey) {
            val scope = when (val resolved = ctx.getScopeByAliasOrFail(alias)) {
                is Either.Left -> return@withIdempotency resolved.value
                is Either.Right -> resolved.value
            }

            // Delete the scope
            val result = ctx.ports.command.deleteScope(DeleteScopeCommand(id = scope.id))

            when (result) {
                is Either.Left -> ctx.services.errors.mapContractError(result.value)
                is Either.Right -> {
                    val json = buildJsonObject {
                        put("canonicalAlias", scope.canonicalAlias)
                        put("deleted", true)
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
