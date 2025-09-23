package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.interfaces.mcp.support.Annotations
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolInput
import io.github.kamiazya.scopes.interfaces.mcp.support.SchemaDsl.toolOutput
import io.github.kamiazya.scopes.interfaces.mcp.support.aliasProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.arrayOfObjectsProperty
import io.github.kamiazya.scopes.interfaces.mcp.support.getScopeByAliasOrFail
import io.github.kamiazya.scopes.interfaces.mcp.support.stringProperty
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Tool handler for getting child scopes.
 *
 * This tool retrieves all child scopes of a parent scope.
 */
class ScopeChildrenToolHandler(private val responseFormatter: ResponseFormatterService = ResponseFormatterService()) : ToolHandler {

    override val name: String = "scopes.children"

    override val description: String = "Get child scopes of a parent scope"

    override val annotations: ToolAnnotations? = Annotations.readOnlyIdempotent()

    override val input: Tool.Input = toolInput(required = listOf("parentAlias")) {
        aliasProperty(name = "parentAlias", description = "Parent scope alias")
    }

    override val output: Tool.Output = toolOutput(required = listOf("parentAlias", "children")) {
        stringProperty("parentAlias")
        arrayOfObjectsProperty("children", itemRequired = listOf("canonicalAlias", "title")) {
            stringProperty("canonicalAlias")
            stringProperty("title")
            stringProperty("description")
        }
    }

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val parentAlias = ctx.services.codec.getString(ctx.args, "parentAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'parentAlias' parameter")

        ctx.services.logger.debug("Getting children of scope: $parentAlias")

        val parent = when (val parentResult = ctx.getScopeByAliasOrFail(parentAlias)) {
            is Either.Left -> return parentResult.value
            is Either.Right -> parentResult.value
        }

        // Get the children
        val result = ctx.ports.query.getChildren(GetChildrenQuery(parentId = parent.id))

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val childrenResult = result.value
                val json = buildJsonObject {
                    put("parentAlias", parentAlias)
                    putJsonArray("children") {
                        childrenResult.scopes.forEach { child ->
                            add(
                                buildJsonObject {
                                    put("canonicalAlias", child.canonicalAlias)
                                    put("title", child.title)
                                    child.description?.let { put("description", it) }
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
