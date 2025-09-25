package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
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
 * Tool handler for setting canonical alias (camelCase).
 *
 * This tool sets the canonical alias for a scope with idempotency support.
 * This is the primary version using camelCase naming convention.
 */
class AliasesSetCanonicalCamelToolHandler : ToolHandler {

    override val name: String = "aliases.setCanonical"

    override val description: String = "Set canonical alias"

    override val annotations: ToolAnnotations? = Annotations.destructiveNonIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("scopeAlias", "newCanonicalAlias")) {
        scopeAliasProperty()
        stringProperty("newCanonicalAlias", minLength = 1, description = "Alias to make canonical")
        idempotencyKeyProperty()
    }

    override val output: Tool.Output = toolOutput(required = listOf("scopeAlias", "newCanonicalAlias")) {
        stringProperty("scopeAlias")
        stringProperty("newCanonicalAlias")
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")

        val newCanonicalAlias = ctx.services.codec.getString(ctx.args, "newCanonicalAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'newCanonicalAlias' parameter")

        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Setting canonical alias for scope: $scopeAlias to: $newCanonicalAlias")

        return ctx.withIdempotency(name, idempotencyKey) {
            val scope = when (val resolved = ctx.getScopeByAliasOrFail(scopeAlias)) {
                is Either.Left -> return@withIdempotency resolved.value
                is Either.Right -> resolved.value
            }

            val result = ctx.ports.command.setCanonicalAlias(
                SetCanonicalAliasCommand(scopeId = scope.id, aliasName = newCanonicalAlias),
            )

            when (result) {
                is Either.Left -> ctx.services.errors.mapContractError(result.value)
                is Either.Right -> {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("newCanonicalAlias", newCanonicalAlias)
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
