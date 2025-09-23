package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.getScopeByAliasOrFail
import io.github.kamiazya.scopes.interfaces.mcp.support.idempotencyKeyProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.newAliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.scopeAliasProperty
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
 * Tool handler for adding aliases to scopes.
 *
 * This tool adds a new alias to an existing scope.
 */
class AliasesAddToolHandler : ToolHandler {

    override val name: String = "aliases.add"

    override val description: String = "Add alias to scope"

    override val annotations: ToolAnnotations? = Annotations.destructiveNonIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("scopeAlias", "newAlias")) {
        scopeAliasProperty()
        newAliasProperty()
        // keep same semantics
        putJsonObject("makeCanonical") {
            put("type", "boolean")
            put("description", "Make this the canonical alias (optional, default false)")
        }
        idempotencyKeyProperty()
    }

    override val output: Tool.Output = toolOutput(required = listOf("scopeAlias", "newAlias", "isCanonical")) {
        putJsonObject("scopeAlias") { put("type", "string") }
        putJsonObject("newAlias") { put("type", "string") }
        putJsonObject("isCanonical") { put("type", "boolean") }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")

        val newAlias = ctx.services.codec.getString(ctx.args, "newAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'newAlias' parameter")

        val makeCanonical = ctx.services.codec.getBoolean(ctx.args, "makeCanonical")
        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Adding alias '$newAlias' to scope: $scopeAlias (makeCanonical: $makeCanonical)")

        return ctx.withIdempotency(name, idempotencyKey) {
            val scope = when (val resolved = ctx.getScopeByAliasOrFail(scopeAlias)) {
                is Either.Left -> return@withIdempotency resolved.value
                is Either.Right -> resolved.value
            }

            // Add the alias
            val addResult = ctx.ports.command.addAlias(
                AddAliasCommand(scopeId = scope.id, aliasName = newAlias),
            )

            when (addResult) {
                is Either.Left -> ctx.services.errors.mapContractError(addResult.value)
                is Either.Right -> {
                    // If makeCanonical is true, set it as canonical
                    if (makeCanonical) {
                        val setCanonicalResult = ctx.ports.command.setCanonicalAlias(
                            SetCanonicalAliasCommand(scopeId = scope.id, aliasName = newAlias),
                        )
                        when (setCanonicalResult) {
                            is Either.Left -> return@withIdempotency ctx.services.errors.mapContractError(setCanonicalResult.value)
                            is Either.Right -> Unit // Continue
                        }
                    }

                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("newAlias", newAlias)
                        put("isCanonical", makeCanonical)
                    }
                    ctx.services.errors.successResult(json.toString())
                }
            }
        }
    }
}
