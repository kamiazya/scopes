package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for getting all root scopes.
 *
 * This tool retrieves all scopes that have no parent (root scopes).
 */
class ScopesRootsToolHandler : ToolHandler {

    override val name: String = "scopes.roots"

    override val description: String = "Get all root scopes (scopes without parents)"

    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                // No parameters required for roots - gets all root scopes
            }
            putJsonObject("properties") {
                // No properties required
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("roots") {
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
            putJsonArray("required") { add("roots") }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        ctx.services.logger.debug("Getting root scopes")

        val result = ctx.ports.query.getRootScopes(GetRootScopesQuery())

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val rootsResult = result.value
                val json = buildJsonObject {
                    putJsonArray("roots") {
                        rootsResult.scopes.forEach { scope ->
                            add(
                                buildJsonObject {
                                    put("canonicalAlias", scope.canonicalAlias)
                                    put("title", scope.title)
                                    scope.description?.let { put("description", it) }
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
