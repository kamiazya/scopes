package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.getScopeByAliasOrFail
import io.github.kamiazya.scopes.interfaces.mcp.support.idempotencyKeyProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.scopeAliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.stringProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.withIdempotency
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Tool handler for removing aliases from scopes.
 *
 * This tool removes an alias from a scope (cannot remove canonical alias).
 */
class AliasesRemoveToolHandler : ToolHandler {

    override val name: String = "aliases.remove"

    override val description: String = "Remove alias (cannot remove canonical)"

    override val annotations: ToolAnnotations? = Annotations.destructiveNonIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("scopeAlias", "aliasToRemove")) {
        scopeAliasProperty(description = "Scope alias to look up")
        stringProperty("aliasToRemove", minLength = 1, description = "Alias to remove (cannot be canonical)")
        idempotencyKeyProperty()
    }

    override val output: Tool.Output = toolOutput(required = listOf("scopeAlias", "removedAlias")) {
        stringProperty("scopeAlias")
        stringProperty("removedAlias")
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")

        val aliasToRemove = ctx.services.codec.getString(ctx.args, "aliasToRemove", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'aliasToRemove' parameter")

        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Removing alias '$aliasToRemove' from scope: $scopeAlias")

        return ctx.withIdempotency(name, idempotencyKey) {
            val scope = when (val resolved = ctx.getScopeByAliasOrFail(scopeAlias)) {
                is Either.Left -> return@withIdempotency resolved.value
                is Either.Right -> resolved.value
            }

            // Remove the alias
            val result = ctx.ports.command.removeAlias(
                RemoveAliasCommand(scopeId = scope.id, aliasName = aliasToRemove),
            )

            when (result) {
                is Either.Left -> ctx.services.errors.mapContractError(result.value)
                is Either.Right -> {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("removedAlias", aliasToRemove)
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
