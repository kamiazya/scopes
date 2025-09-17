package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.ToolAnnotations
import kotlinx.serialization.json.*

/**
 * Tool handler for getting all root scopes.
 *
 * This tool retrieves all scopes that have no parent (root scopes).
 */
class ScopesRootsToolHandler(private val responseFormatter: ResponseFormatterService = ResponseFormatterService()) : ToolHandler {

    private fun Map<*, *>.toJsonObject(): JsonObject = buildJsonObject {
        forEach { (k, v) ->
            val key = k.toString()
            when (v) {
                is Map<*, *> -> putJsonObject(key) {
                    v.forEach { (innerK, innerV) ->
                        put(innerK.toString(), JsonPrimitive(innerV.toString()))
                    }
                }
                is Number -> put(key, JsonPrimitive(v))
                is Boolean -> put(key, JsonPrimitive(v))
                is String -> put(key, JsonPrimitive(v))
                null -> put(key, JsonNull)
                else -> put(key, JsonPrimitive(v.toString()))
            }
        }
    }

    override val name: String = "scopes.roots"

    override val description: String = "Get all root scopes (scopes without parents)"

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
                val responseMap = responseFormatter.formatRootScopesForMcp(result.value)
                // Convert Map to JSON string
                val json = buildJsonObject {
                    responseMap.forEach { (key, value) ->
                        when (value) {
                            is List<*> -> putJsonArray(key) {
                                value.forEach { item ->
                                    when (item) {
                                        is Map<*, *> -> add(item.toJsonObject())
                                        else -> add(JsonPrimitive(item.toString()))
                                    }
                                }
                            }
                            is Map<*, *> -> putJsonObject(key) {
                                value.forEach { (k, v) ->
                                    put(k.toString(), JsonPrimitive(v.toString()))
                                }
                            }
                            is Number -> put(key, JsonPrimitive(value))
                            is Boolean -> put(key, JsonPrimitive(value))
                            is String -> put(key, JsonPrimitive(value))
                            else -> put(key, JsonPrimitive(value.toString()))
                        }
                    }
                }
                ctx.services.errors.successResult(json.toString())
            }
        }
    }
}
