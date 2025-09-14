package io.github.kamiazya.scopes.interfaces.providers

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Factory that builds a configured MCP Server with capabilities and registered tools/resources/prompts.
 * Alias-first: all tools accept alias (no ULID exposure).
 */
class McpServerFactory(
    private val logger: Logger,
    private val scopeQueryPort: ScopeManagementQueryPort,
    private val scopeCommandPort: ScopeManagementCommandPort,
) {
    fun create(): Server {
        val server = Server(
            Implementation(name = "scopes", version = "0.1.0"),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                ),
            ),
        )

        registerTools(server)
        registerResources(server)
        registerPrompts(server)

        return server
    }

    private fun registerTools(server: Server) {
        // aliases.resolve (exact only for now)
        server.addTool(
            name = "aliases.resolve",
            description = "Resolve a scope by alias (exact match). Returns canonical alias if found.",
            inputSchema = Tool.Input(),
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val result = runCatching {
                kotlinx.coroutines.runBlocking {
                    scopeQueryPort.getScopeByAlias(
                        io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery(alias),
                    )
                }
            }.getOrElse { return@addTool errorResult(it.message ?: "Resolution failed") }

            result.fold(
                { error -> errorResult(mapContractError(error)) },
                { scope ->
                    val payload = buildJsonObject {
                        put("alias", JsonPrimitive(scope.canonicalAlias))
                        put("title", JsonPrimitive(scope.title))
                        // keep ID private by default
                    }
                    CallToolResult(content = listOf(TextContent(payload.toString())))
                },
            )
        }

        // scopes.get (by alias)
        server.addTool(
            name = "scopes.get",
            description = "Get a scope by alias (exact match)",
            inputSchema = Tool.Input(),
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val res = kotlinx.coroutines.runBlocking {
                scopeQueryPort.getScopeByAlias(
                    io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery(alias),
                )
            }
            res.fold(
                { error -> errorResult(mapContractError(error)) },
                { scope ->
                    val json = buildJsonObject {
                        put("canonicalAlias", scope.canonicalAlias)
                        put("title", scope.title)
                        scope.description?.let { put("description", it) }
                        scope.parentId?.let { _ -> /* intentionally hidden */ }
                        put("createdAt", scope.createdAt.toString())
                        put("updatedAt", scope.updatedAt.toString())
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                },
            )
        }
    }

    private fun registerResources(server: Server) {
        // Minimal static resource for now
        server.addResource(
            uri = "scopes:/docs/cli",
            name = "CLI Quick Reference",
            description = "Scopes CLI quick reference",
            mimeType = "text/markdown; charset=utf-8",
        ) {
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = "See repository docs/reference/cli-quick-reference.md",
                        uri = it.uri,
                        mimeType = "text/markdown; charset=utf-8",
                    ),
                ),
            )
        }
    }

    private fun registerPrompts(server: Server) {
        server.addPrompt(
            name = "prompts.scopes.summarize",
            description = "Summarize a scope by alias",
            arguments = listOf(
                PromptArgument(name = "alias", description = "Scope alias", required = true),
                PromptArgument(name = "level", description = "Summary level", required = false),
            ),
        ) { req ->
            val alias = req.arguments?.get("alias") ?: ""
            GetPromptResult(
                description = "Summarize scope",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("Summarize the scope <alias>$alias</alias> concisely."),
                    ),
                ),
            )
        }
    }

    private fun err(message: String): CallToolResult = CallToolResult(content = listOf(TextContent(message)), isError = true)
    private fun errorResult(message: String): CallToolResult = CallToolResult(content = listOf(TextContent(message)), isError = true)
    private fun mapContractError(error: ScopeContractError): String = when (error) {
        is ScopeContractError.BusinessError.NotFound -> "Scope not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Duplicate alias: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateTitle -> "Duplicate title"
        is ScopeContractError.InputError.InvalidId -> "Invalid id: ${error.id}"
        is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout -> "Timeout: ${error.operation}"
        is ScopeContractError.SystemError.ConcurrentModification -> "Concurrent modification"
        is ScopeContractError.InputError.InvalidTitle -> "Invalid title"
        is ScopeContractError.InputError.InvalidDescription -> "Invalid description"
        is ScopeContractError.InputError.InvalidParentId -> "Invalid parent id"
        is ScopeContractError.BusinessError.HierarchyViolation -> "Hierarchy violation"
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted"
        is ScopeContractError.BusinessError.ArchivedScope -> "Archived scope"
        is ScopeContractError.BusinessError.NotArchived -> "Not archived"
        is ScopeContractError.BusinessError.HasChildren -> "Has children"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias"
        else -> "Error"
    }
}
