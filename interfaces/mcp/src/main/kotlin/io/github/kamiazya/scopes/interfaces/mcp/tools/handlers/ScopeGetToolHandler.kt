package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for getting a scope by alias.
 *
 * This tool retrieves detailed information about a scope given its alias.
 */
class ScopeGetToolHandler(private val responseFormatter: ResponseFormatterService = ResponseFormatterService()) : ToolHandler {

    override val name: String = "scopes.get"

    override val description: String = "Get a scope by alias (exact match)"

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
                    put("description", "Scope alias to look up")
                }
            }
        },
    )

    override val annotations: ToolAnnotations? = ToolAnnotations(
        title = null,
        readOnlyHint = true,
        destructiveHint = false,
        idempotentHint = true,
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("canonicalAlias") { put("type", "string") }
                putJsonObject("title") { put("type", "string") }
                putJsonObject("description") { put("type", "string") }
                putJsonObject("createdAt") { put("type", "string") }
                putJsonObject("updatedAt") { put("type", "string") }
            }
            putJsonArray("required") {
                add("canonicalAlias")
                add("title")
                add("createdAt")
                add("updatedAt")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val alias = ctx.services.codec.getString(ctx.args, "alias", required = true)
            ?: return ctx.services.errors.errorResult("Missing 'alias' parameter")

        ctx.services.logger.debug("Getting scope by alias: $alias")

        val result = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))

        return when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val responseMap = responseFormatter.formatScopeForMcp(result.value)
                // Convert Map to JSON string
                val json = buildJsonObject {
                    responseMap.forEach { (key, value) ->
                        when (value) {
                            is Map<*, *> -> putJsonObject(key) {
                                value.forEach { (k, v) ->
                                    put(k.toString(), JsonPrimitive(v.toString()))
                                }
                            }
                            is Number -> put(key, JsonPrimitive(value))
                            is Boolean -> put(key, JsonPrimitive(value))
                            is String -> put(key, JsonPrimitive(value))
                            null -> put(key, JsonNull)
                            else -> put(key, JsonPrimitive(value.toString()))
                        }
                    }
                }
                CallToolResult(content = listOf(TextContent(json.toString())))
            }
        }
    }
}
