package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for setting canonical alias (camelCase).
 *
 * This tool sets the canonical alias for a scope with idempotency support.
 * This is the primary version using camelCase naming convention.
 */
class AliasesSetCanonicalCamelToolHandler : ToolHandler {

    override val name: String = "aliases.setCanonical"

    override val description: String = "Set canonical alias"

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
                add("scopeAlias")
                add("newCanonicalAlias")
            }
            putJsonObject("properties") {
                putJsonObject("scopeAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Existing scope alias")
                }
                putJsonObject("newCanonicalAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Alias to make canonical")
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
                putJsonObject("scopeAlias") { put("type", "string") }
                putJsonObject("newCanonicalAlias") { put("type", "string") }
            }
            putJsonArray("required") {
                add("scopeAlias")
                add("newCanonicalAlias")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")

        val newCanonicalAlias = ctx.services.codec.getString(ctx.args, "newCanonicalAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'newCanonicalAlias' parameter")

        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")

        ctx.services.logger.debug("Setting canonical alias for scope: $scopeAlias to: $newCanonicalAlias")

        return ctx.services.idempotency.getOrCompute(name, ctx.args, idempotencyKey) {
            // First get the scope
            val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            val scopeId = when (scopeResult) {
                is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            // Set the canonical alias
            val result = ctx.ports.command.setCanonicalAlias(
                SetCanonicalAliasCommand(scopeId = scopeId, aliasName = newCanonicalAlias),
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
