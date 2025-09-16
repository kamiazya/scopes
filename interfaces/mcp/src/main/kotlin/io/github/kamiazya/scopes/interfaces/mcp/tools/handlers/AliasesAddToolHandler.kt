package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for adding aliases to scopes.
 * 
 * This tool adds a new alias to an existing scope.
 */
class AliasesAddToolHandler : ToolHandler {
    
    override val name: String = "aliases.add"
    
    override val description: String = "Add alias to scope"
    
    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                add("scopeAlias")
                add("newAlias")
            }
            putJsonObject("properties") {
                putJsonObject("scopeAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Existing scope alias")
                }
                putJsonObject("newAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "New alias to add")
                }
                putJsonObject("makeCanonical") {
                    put("type", "boolean")
                    put("description", "Make this the canonical alias (optional, default false)")
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
                putJsonObject("newAlias") { put("type", "string") }
                putJsonObject("isCanonical") { put("type", "boolean") }
            }
            putJsonArray("required") {
                add("scopeAlias")
                add("newAlias")
                add("isCanonical")
            }
        }
    )
    
    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val scopeAlias = ctx.services.codec.getString(ctx.args, "scopeAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'scopeAlias' parameter")
        
        val newAlias = ctx.services.codec.getString(ctx.args, "newAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'newAlias' parameter")
        
        val makeCanonical = ctx.services.codec.getBoolean(ctx.args, "makeCanonical") ?: false
        val idempotencyKey = ctx.services.codec.getString(ctx.args, "idempotencyKey")
        
        ctx.services.logger.debug("Adding alias '$newAlias' to scope: $scopeAlias (makeCanonical: $makeCanonical)")
        
        return ctx.services.idempotency.getOrCompute(name, ctx.args, idempotencyKey) {
            // First get the scope
            val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            val scopeId = when (scopeResult) {
                is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }
            
            // Add the alias
            val addResult = ctx.ports.command.addAlias(
                AddAliasCommand(scopeId = scopeId, aliasName = newAlias)
            )
            
            when (addResult) {
                is Either.Left -> ctx.services.errors.mapContractError(addResult.value)
                is Either.Right -> {
                    // If makeCanonical is true, set it as canonical
                    if (makeCanonical) {
                        val setCanonicalResult = ctx.ports.command.setCanonicalAlias(
                            SetCanonicalAliasCommand(scopeId = scopeId, aliasName = newAlias)
                        )
                        when (setCanonicalResult) {
                            is Either.Left -> return@getOrCompute ctx.services.errors.mapContractError(setCanonicalResult.value)
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