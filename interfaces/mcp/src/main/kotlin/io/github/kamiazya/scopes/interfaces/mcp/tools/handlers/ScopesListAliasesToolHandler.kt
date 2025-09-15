package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for listing all aliases for a scope.
 * 
 * This tool lists all aliases associated with a scope.
 */
class ScopesListAliasesToolHandler : ToolHandler {
    
    override val name: String = "scopes.aliases.list"
    
    override val description: String = "List all aliases for a scope"
    
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
                    put("description", "Scope alias")
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
                putJsonObject("canonicalAlias") { put("type", "string") }
                putJsonObject("aliases") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "object")
                        put("additionalProperties", false)
                        putJsonObject("properties") {
                            putJsonObject("aliasName") { put("type", "string") }
                            putJsonObject("isCanonical") { put("type", "boolean") }
                            putJsonObject("aliasType") { put("type", "string") }
                        }
                        putJsonArray("required") {
                            add("aliasName")
                            add("isCanonical")
                        }
                    }
                }
            }
            putJsonArray("required") {
                add("scopeAlias")
                add("aliases")
            }
        }
    )
    
    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")
        
        ctx.services.logger.debug("Listing aliases for scope: $alias")
        
        // First get the scope to get its ID
        val scopeResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))
        val scopeId = when (scopeResult) {
            is Either.Left -> return ctx.services.errors.mapContractError(scopeResult.value)
            is Either.Right -> scopeResult.value.id
        }
        
        // List all aliases
        val result = ctx.ports.query.listAliases(ListAliasesQuery(scopeId = scopeId))
        
        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val aliasListResult = result.value
                val canonicalAlias = aliasListResult.aliases.find { it.isCanonical }?.aliasName ?: ""
                
                val json = buildJsonObject {
                    put("scopeAlias", alias)
                    put("canonicalAlias", canonicalAlias)
                    putJsonArray("aliases") {
                        aliasListResult.aliases.forEach { aliasInfo ->
                            add(
                                buildJsonObject {
                                    put("aliasName", aliasInfo.aliasName)
                                    put("isCanonical", aliasInfo.isCanonical)
                                    put("aliasType", aliasInfo.aliasType)
                                }
                            )
                        }
                    }
                }
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}