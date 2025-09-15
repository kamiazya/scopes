package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * Tool handler for creating new scopes.
 *
 * This tool creates a new scope with optional parent, custom alias, and idempotency support.
 */
class ScopeCreateToolHandler : ToolHandler {

    override val name: String = "scopes.create"

    override val description: String = "Create a new scope. Parent can be specified by alias."

    override val input: Tool.Input = Tool.Input(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonArray("required") {
                add("title")
            }
            putJsonObject("properties") {
                putJsonObject("title") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Scope title")
                }
                putJsonObject("description") {
                    put("type", "string")
                    put("description", "Optional scope description")
                }
                putJsonObject("parentAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Parent scope alias (optional)")
                }
                putJsonObject("customAlias") {
                    put("type", "string")
                    put("minLength", 1)
                    put("description", "Custom alias instead of generated one (optional)")
                }
                putJsonObject("idempotencyKey") {
                    put("type", "string")
                    put("pattern", "[a-zA-Z0-9._-]{1,255}")
                    put("description", "Idempotency key to prevent duplicate operations")
                }
            }
        },
    )

    override val output: Tool.Output = Tool.Output(
        properties = buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            putJsonObject("properties") {
                putJsonObject("canonicalAlias") { put("type", "string") }
                putJsonObject("title") { put("type", "string") }
                putJsonObject("description") { put("type", "string") }
                putJsonObject("parentAlias") { put("type", "string") }
                putJsonObject("createdAt") { put("type", "string") }
            }
            putJsonArray("required") {
                add("canonicalAlias")
                add("title")
                add("createdAt")
            }
        },
    )

    override suspend fun handle(ctx: ToolContext): CallToolResult {
        val title = ctx.args["title"]?.jsonPrimitive?.content
            ?: return ctx.services.errors.errorResult("Missing 'title' parameter")

        val description = ctx.args["description"]?.jsonPrimitive?.content
        val parentAlias = ctx.args["parentAlias"]?.jsonPrimitive?.content
        val customAlias = ctx.args["customAlias"]?.jsonPrimitive?.content
        val idempotencyKey = ctx.args["idempotencyKey"]?.jsonPrimitive?.content

        ctx.services.logger.debug("Creating scope: $title (parentAlias: $parentAlias, customAlias: $customAlias)")

        // Check idempotency if key provided
        val cachedResult = ctx.services.idempotency.checkIdempotency(name, ctx.args, idempotencyKey)
        if (cachedResult != null) {
            ctx.services.logger.debug("Returning cached result for idempotency key: $idempotencyKey")
            return cachedResult
        }

        // Resolve parent ID if parent alias provided
        val parentId = if (parentAlias != null) {
            val parentResult = ctx.ports.query.getScopeByAlias(GetScopeByAliasQuery(parentAlias))
            when (parentResult) {
                is Either.Left -> return ctx.services.errors.mapContractError(parentResult.value)
                is Either.Right -> parentResult.value.id
            }
        } else {
            null
        }

        val result = ctx.ports.command.createScope(
            CreateScopeCommand(
                title = title,
                description = description,
                parentId = parentId,
                generateAlias = customAlias == null,
                customAlias = customAlias,
            ),
        )

        val toolResult = when (result) {
            is Either.Left -> ctx.services.errors.mapContractError(result.value)
            is Either.Right -> {
                val created = result.value
                val json = buildJsonObject {
                    put("canonicalAlias", created.canonicalAlias)
                    put("title", created.title)
                    created.description?.let { put("description", it) }
                    parentAlias?.let { put("parentAlias", it) }
                    put("createdAt", created.createdAt.toString())
                }
                CallToolResult(content = listOf(TextContent(json.toString())))
            }
        }

        // Store result for idempotency if key was provided
        ctx.services.idempotency.storeIdempotency(name, ctx.args, toolResult, idempotencyKey)

        return toolResult
    }
}
