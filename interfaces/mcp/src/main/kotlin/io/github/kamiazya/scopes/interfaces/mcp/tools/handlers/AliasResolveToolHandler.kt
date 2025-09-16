package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for resolving aliases to scopes.
 *
 * This tool resolves a scope by alias (exact match only) and returns
 * basic scope information including the canonical alias.
 */
class AliasResolveToolHandler : ToolHandler {

    override val name: String = "aliases.resolve"

    override val description: String = "Resolve a scope by alias (exact match only). Returns the canonical alias if found."

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
                add("alias")
            }
            putJsonObject("properties") {
                putJsonObject("alias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Alias to resolve (exact match only)")
                }
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("alias") {
                    put("type", "string")
                    put("description", "The input alias provided by the user")
                }
                putJsonObject("canonicalAlias") {
                    put("type", "string")
                    put("description", "The canonical (normalized) alias of the scope")
                }
                putJsonObject("title") {
                    put("type", "string")
                    put("description", "The title of the resolved scope")
                }
            }
            putJsonArray("required") {
                add("alias")
                add("canonicalAlias")
                add("title")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        ctx.services.logger.debug("Resolving alias: $alias")

        val result = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val scope = result.value
                val json = buildJsonObject {
                    put("alias", alias) // Input alias
                    put("canonicalAlias", scope.canonicalAlias) // Normalized alias
                    put("title", scope.title)
                }
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}
