package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for removing aliases from scopes.
 * 
 * This tool removes an alias from a scope (cannot remove canonical alias).
 */
class AliasesRemoveToolHandler : ToolHandler {
    
    override val name: String = "aliases.remove"
    
    override val description: String = "Remove alias (cannot remove canonical)"
    
    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                add("scopeAlias")
                add("aliasToRemove")
            }
            putJsonObject("properties") {
                putJsonObject("scopeAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Scope alias to look up")
                }
                putJsonObject("aliasToRemove") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Alias to remove (cannot be canonical)")
                }
                putJsonObject("idempotencyKey") {
                    put("type", "string")
                    put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                    put("description", "Idempotency key to prevent duplicate operations")
                }
            }
        }
    )
    
    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("scopeAlias") { put("type", "string") }
                putJsonObject("removedAlias") { put("type", "string") }
            }
            putJsonArray("required") {
                add("scopeAlias")
                add("removedAlias")
            }
        }
    )
    
    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")
        
        val aliasToRemove = ctx.services.codec.getString(ctx.args, "aliasToRemove", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'aliasToRemove' parameter")
        
        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")
        
        ctx.services.logger.debug("Removing alias '$aliasToRemove' from scope: $scopeAlias")
        
        return ctx.services.idempotency.getOrCompute(name, ctx.args, idempotencyKey) {
            // First get the scope
            val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            val scopeId = when (scopeResult) {
                is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }
            
            // Remove the alias
            val result = ctx.ports.command.removeAlias(
                RemoveAliasCommand(scopeId = scopeId, aliasName = aliasToRemove)
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