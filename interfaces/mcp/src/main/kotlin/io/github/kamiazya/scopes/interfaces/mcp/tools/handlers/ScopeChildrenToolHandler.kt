package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for getting child scopes.
 *
 * This tool retrieves all child scopes of a parent scope.
 */
class ScopeChildrenToolHandler : ToolHandler {

    override val name: String = "scopes.children"

    override val description: String = "Get child scopes of a parent scope"

    override val annotations: ToolAnnotations? = ToolAnnotations(
        title = null,
        readOnlyHint = true,
        destructiveHint = false,
        idempotentHint = true,
    )

    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                add("parentAlias")
            }
            putJsonObject("properties") {
                putJsonObject("parentAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Parent scope alias")
                }
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("parentAlias") { put("type", "string") }
                putJsonObject("children") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "object")
                        put("additionalProperties", false)
                        putJsonObject("properties") {
                            putJsonObject("canonicalAlias") { put("type", "string") }
                            putJsonObject("title") { put("type", "string") }
                            putJsonObject("description") { put("type", "string") }
                        }
                        putJsonArray("required") {
                            add("canonicalAlias")
                            add("title")
                        }
                    }
                }
            }
            putJsonArray("required") {
                add("parentAlias")
                add("children")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val parentAlias = ctx.services.codec.getString(ctx.args, "parentAlias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'parentAlias' parameter")

        ctx.services.logger.debug("Getting children of scope: $parentAlias")

        // First get the parent scope to get its ID
        val parentResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(parentAlias))
        val parentId = when (parentResult) {
            is Either.Left -> return ctx.services.errors.mapContractError(parentResult.value)
            is Either.Right -> parentResult.value.id
        }

        // Get the children
        val result = ctx.ports.query.getChildren(GetChildrenQuery(parentId = parentId))

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
