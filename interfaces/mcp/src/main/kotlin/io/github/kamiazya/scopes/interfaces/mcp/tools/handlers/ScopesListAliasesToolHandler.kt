package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.aliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.arrayProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.booleanProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.itemsObject
import io.github.kamiazya.scopes.interfaces.mcp.support.stringProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Tool handler for listing all aliases for a scope.
 *
 * This tool lists all aliases associated with a scope.
 */
class ScopesListAliasesToolHandler : ToolHandler {

    override val name: String = "scopes.aliases.list"

    override val description: String = "List all aliases for a scope"

    override val annotations: ToolAnnotations? = Annotations.readOnlyIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("alias")) {
        aliasProperty(description = "Scope alias")
    }

    override val output: Tool.Output = toolOutput(required = listOf("scopeAlias", "aliases")) {
        stringProperty("scopeAlias")
        stringProperty("canonicalAlias")
        arrayProperty("aliases") {
            itemsObject(required = listOf("aliasName", "isCanonical")) {
                stringProperty("aliasName")
                booleanProperty("isCanonical")
                stringProperty("aliasType")
            }
        }
    }

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
                                },
                            )
                        }
                    }
                }
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}
