package io.github.kamiazya.scopes.interfaces.mcp.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.*
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.time.Duration.Companion.minutes

/**
 * Stores idempotency results with TTL.
 */
private data class StoredResult(
    val result: CallToolResult,
    val timestamp: Instant
)

/**
 * MCP Server Adapter that provides an MCP interface to Scopes functionality.
 * This is a pure adapter without any DI concerns, following Clean Architecture principles.
 */
class McpServerAdapter(
    private val scopeQueryPort: ScopeManagementQueryPort,
    private val scopeCommandPort: ScopeManagementCommandPort
) {
    // Idempotency store: toolName|idempotencyKey|argsHash -> StoredResult
    // Note: In production, use a thread-safe implementation
    private val idempotencyStore: MutableMap<String, StoredResult> = mutableMapOf()
    private val idempotencyTtlMinutes = 10L

    companion object {
        private val IDEMPOTENCY_KEY_PATTERN = Regex("^[A-Za-z0-9_-]{8,128}$")
    }

    /**
     * Run the MCP server on stdio transport.
     * This is the main entry point for the MCP server.
     */
    fun runStdio(
        inputStream: java.io.InputStream = System.`in`,
        outputStream: java.io.OutputStream = System.out
    ) {
        val server = createServer()
        val transport = StdioServerTransport(
            inputStream = java.io.BufferedInputStream(inputStream),
            outputStream = java.io.PrintStream(outputStream, true)
        )

        runBlocking {
            try {
                System.err.println("[MCP] Starting server...")
                server.connect(transport)
                System.err.println("[MCP] Server connected successfully")

                // Keep process alive; MCP client typically terminates the process when done.
                try {
                    while (true) kotlinx.coroutines.delay(60_000)
                } catch (_: Throwable) {
                    // exit gracefully
                }
            } catch (e: Exception) {
                System.err.println("[MCP] Failed to start server: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Create a server configured for testing purposes.
     * This method exposes server configuration for integration tests.
     */
    fun createTestServer(): Server {
        val server = Server(
            Implementation(name = "scopes-test", version = "0.1.0"),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    prompts = ServerCapabilities.Prompts(listChanged = true)
                )
            )
        )

        registerTools(server)
        registerResources(server)
        registerPrompts(server)

        return server
    }

    private fun createServer(): Server {
        val server = Server(
            Implementation(name = "scopes", version = "0.1.0"),
            ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    prompts = ServerCapabilities.Prompts(listChanged = true)
                )
            )
        )

        registerTools(server)
        registerResources(server)
        registerPrompts(server)

        return server
    }

    private fun registerTools(server: Server) {
        // aliases.resolve
        server.addTool(
            name = "aliases.resolve",
            description = "Resolve a scope by alias (exact match). Returns canonical alias if found.",
            inputSchema = Tool.Input(
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
                            put("description", "Alias to resolve")
                        }
                        putJsonObject("match") {
                            put("type", "string")
                            putJsonArray("enum") {
                                add("auto")
                                add("exact")
                                add("prefix")
                            }
                            put("default", "auto")
                            put("description", "Matching mode: auto (exact then prefix), exact, or prefix")
                        }
                    }
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val result = runCatching {
                runBlocking {
                    scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
                }
            }.getOrElse { return@addTool err(it.message ?: "Resolution failed") }

            result.fold(
                { error -> errorResult(error) },
                { scope ->
                    val payload = buildJsonObject {
                        put("alias", scope.canonicalAlias)
                        put("title", scope.title)
                    }
                    CallToolResult(content = listOf(TextContent(payload.toString())))
                }
            )
        }

        // scopes.get
        server.addTool(
            name = "scopes.get",
            description = "Get a scope by alias (exact match)",
            inputSchema = Tool.Input(
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
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val res = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            res.fold(
                { error -> errorResult(error) },
                { scope ->
                    val json = buildJsonObject {
                        put("canonicalAlias", scope.canonicalAlias)
                        put("title", scope.title)
                        scope.description?.let { put("description", it) }
                        put("createdAt", scope.createdAt.toString())
                        put("updatedAt", scope.updatedAt.toString())
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }

        // scopes.create
        server.addTool(
            name = "scopes.create",
            description = "Create a new scope. Parent can be specified by alias.",
            inputSchema = Tool.Input(
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
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val title = req.arguments["title"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'title'")
            val description = req.arguments["description"]?.jsonPrimitive?.content
            val parentAlias = req.arguments["parentAlias"]?.jsonPrimitive?.content
            val customAlias = req.arguments["customAlias"]?.jsonPrimitive?.content
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Check idempotency
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("scopes.create", key, req.arguments)
                if (cached != null) return@addTool cached
            }

            // Resolve parent ID if parent alias provided
            val parentId = if (parentAlias != null) {
                val parentResult = runBlocking {
                    scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(parentAlias))
                }
                when (parentResult) {
                    is Either.Left -> return@addTool errorResult(parentResult.value)
                    is Either.Right -> parentResult.value.id
                }
            } else {
                null
            }

            val result = runBlocking {
                scopeCommandPort.createScope(
                    CreateScopeCommand(
                        title = title,
                        description = description,
                        parentId = parentId,
                        generateAlias = customAlias == null,
                        customAlias = customAlias
                    )
                )
            }

            val toolResult = result.fold(
                { error -> errorResult(error) },
                { created ->
                    val json = buildJsonObject {
                        put("canonicalAlias", created.canonicalAlias)
                        put("title", created.title)
                        created.description?.let { put("description", it) }
                        parentAlias?.let { put("parentAlias", it) }
                        put("createdAt", created.createdAt.toString())
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )

            // Store result for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("scopes.create", key, req.arguments, toolResult)
            }

            toolResult
        }

        // Additional tools registration continues...
        registerUpdateTool(server)
        registerDeleteTool(server)
        registerChildrenTool(server)
        registerRootsTool(server)
        registerAliasingTools(server)
    }

    private fun registerUpdateTool(server: Server) {
        server.addTool(
            name = "scopes.update",
            description = "Update an existing scope's title or description",
            inputSchema = Tool.Input(
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
                            put("description", "Scope alias to update")
                        }
                        putJsonObject("title") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "New title (optional)")
                        }
                        putJsonObject("description") {
                            put("type", "string")
                            put("description", "New description (optional)")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val title = req.arguments["title"]?.jsonPrimitive?.content
            val description = req.arguments["description"]?.jsonPrimitive?.content

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeCommandPort.updateScope(
                    UpdateScopeCommand(
                        id = scopeId,
                        title = title,
                        description = description
                    )
                )
            }

            result.fold(
                { error -> errorResult(error) },
                { updated ->
                    val json = buildJsonObject {
                        put("alias", alias)
                        put("title", updated.title)
                        updated.description?.let { put("description", it) }
                        put("updatedAt", updated.updatedAt.toString())
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }
    }

    private fun registerDeleteTool(server: Server) {
        server.addTool(
            name = "scopes.delete",
            description = "Delete a scope (must have no children)",
            inputSchema = Tool.Input(
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
                            put("description", "Scope alias to delete")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeCommandPort.deleteScope(DeleteScopeCommand(id = scopeId))
            }

            result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("alias", alias)
                        put("deleted", true)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }
    }

    private fun registerChildrenTool(server: Server) {
        server.addTool(
            name = "scopes.children",
            description = "Get child scopes of a parent scope",
            inputSchema = Tool.Input(
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
                }
            )
        ) { req ->
            val parentAlias = req.arguments["parentAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'parentAlias'")

            val parentResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(parentAlias))
            }
            val parentId = when (parentResult) {
                is Either.Left -> return@addTool errorResult(parentResult.value)
                is Either.Right -> parentResult.value.id
            }

            val result = runBlocking {
                scopeQueryPort.getChildren(GetChildrenQuery(parentId = parentId))
            }

            result.fold(
                { error -> errorResult(error) },
                { childrenResult ->
                    val json = buildJsonObject {
                        put("parentAlias", parentAlias)
                        putJsonArray("children") {
                            childrenResult.scopes.forEach { child ->
                                add(
                                    buildJsonObject {
                                        put("canonicalAlias", child.canonicalAlias)
                                        put("title", child.title)
                                        child.description?.let { put("description", it) }
                                    }
                                )
                            }
                        }
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }
    }

    private fun registerRootsTool(server: Server) {
        server.addTool(
            name = "scopes.roots",
            description = "Get all root scopes (scopes without parents)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonArray("required") {
                        // No parameters required for roots - gets all root scopes
                    }
                    putJsonObject("properties") {
                        // No properties required
                    }
                }
            )
        ) { req ->
            val result = runBlocking {
                scopeQueryPort.getRootScopes(GetRootScopesQuery())
            }

            result.fold(
                { error -> errorResult(error) },
                { rootsResult ->
                    val json = buildJsonObject {
                        putJsonArray("roots") {
                            rootsResult.scopes.forEach { scope ->
                                add(
                                    buildJsonObject {
                                        put("canonicalAlias", scope.canonicalAlias)
                                        put("title", scope.title)
                                        scope.description?.let { put("description", it) }
                                    }
                                )
                            }
                        }
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }
    }

    private fun registerAliasingTools(server: Server) {
        // aliases.add
        server.addTool(
            name = "aliases.add",
            description = "Add a new alias to an existing scope",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonArray("required") {
                        add("scopeAlias")
                        add("newAlias")
                    }
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Existing scope alias")
                        }
                        putJsonObject("newAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "New alias to add")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val newAlias = req.arguments["newAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'newAlias'")

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeCommandPort.addAlias(AddAliasCommand(scopeId = scopeId, aliasName = newAlias))
            }

            result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("addedAlias", newAlias)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }

        // aliases.remove
        server.addTool(
            name = "aliases.remove",
            description = "Remove an alias from a scope (cannot remove canonical alias)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonArray("required") {
                        add("scopeAlias")
                        add("aliasToRemove")
                    }
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Existing scope alias")
                        }
                        putJsonObject("aliasToRemove") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Alias to remove")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val aliasToRemove = req.arguments["aliasToRemove"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'aliasToRemove'")

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeCommandPort.removeAlias(RemoveAliasCommand(scopeId = scopeId, aliasName = aliasToRemove))
            }

            result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("removedAlias", aliasToRemove)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }

        // aliases.setCanonical
        server.addTool(
            name = "aliases.setCanonical",
            description = "Set which alias should be the canonical (primary) alias for a scope",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonArray("required") {
                        add("scopeAlias")
                        add("newCanonicalAlias")
                    }
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Existing scope alias")
                        }
                        putJsonObject("newCanonicalAlias") {
                            put("type", "string")
                            put("minLength", 1)
                            put("description", "Alias to make canonical")
                        }
                        putJsonObject("idempotencyKey") {
                            put("type", "string")
                            put("pattern", "^[A-Za-z0-9_-]{8,128}$")
                            put("description", "Idempotency key for avoiding duplicate operations")
                        }
                    }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val newCanonicalAlias = req.arguments["newCanonicalAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'newCanonicalAlias'")

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(scopeAlias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeCommandPort.setCanonicalAlias(SetCanonicalAliasCommand(scopeId = scopeId, aliasName = newCanonicalAlias))
            }

            result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeId", scopeId)
                        put("newCanonicalAlias", newCanonicalAlias)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }

        // scopes.aliases.list
        server.addTool(
            name = "scopes.aliases.list",
            description = "List all aliases for a scope",
            inputSchema = Tool.Input(
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
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            val scopeId = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id
            }

            val result = runBlocking {
                scopeQueryPort.listAliases(ListAliasesQuery(scopeId = scopeId))
            }

            result.fold(
                { error -> errorResult(error) },
                { aliasListResult ->
                    val json = buildJsonObject {
                        put("scopeAlias", alias)
                        val canonicalAlias = aliasListResult.aliases.find { it.isCanonical }?.aliasName ?: ""
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
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
        }
    }

    private fun registerResources(server: Server) {
        // Minimal static resource for now
        server.addResource(
            uri = "scopes:/docs/cli",
            name = "CLI Quick Reference",
            description = "Scopes CLI quick reference",
            mimeType = "text/markdown; charset=utf-8"
        ) {
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = "See repository docs/reference/cli-quick-reference.md",
                        uri = it.uri,
                        mimeType = "text/markdown; charset=utf-8"
                    )
                )
            )
        }
    }

    private fun registerPrompts(server: Server) {
        server.addPrompt(
            name = "prompts.scopes.summarize",
            description = "Summarize a scope by alias",
            arguments = listOf(
                PromptArgument(name = "alias", description = "Scope alias", required = true),
                PromptArgument(name = "level", description = "Summary level", required = false)
            )
        ) { req ->
            val alias = req.arguments?.get("alias") ?: ""
            GetPromptResult(
                description = "Summarize scope",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("Summarize the scope <alias>$alias</alias> concisely.")
                    )
                )
            )
        }
    }

    private fun err(message: String): CallToolResult = CallToolResult(content = listOf(TextContent(message)), isError = true)

    private fun errorResult(error: ScopeContractError): CallToolResult {
        val errorData = buildJsonObject {
            put("code", getErrorCode(error))
            putJsonObject("data") {
                put("type", error::class.simpleName)
                put("message", mapContractError(error))
                when (error) {
                    is ScopeContractError.BusinessError.AliasNotFound -> {
                        put("alias", error.alias)
                    }
                    is ScopeContractError.BusinessError.DuplicateAlias -> {
                        put("alias", error.alias)
                    }
                    is ScopeContractError.BusinessError.DuplicateTitle -> {
                        put("title", error.title)
                        error.existingScopeId?.let { put("existingScopeId", it) }
                    }
                    else -> Unit
                }
            }
        }
        return CallToolResult(content = listOf(TextContent(errorData.toString())), isError = true)
    }

    private fun getErrorCode(error: ScopeContractError): Int = when (error) {
        is ScopeContractError.InputError -> -32602 // Invalid params
        is ScopeContractError.BusinessError.NotFound,
        is ScopeContractError.BusinessError.AliasNotFound -> -32011 // Not found
        is ScopeContractError.BusinessError.DuplicateAlias,
        is ScopeContractError.BusinessError.DuplicateTitle -> -32012 // Duplicate
        is ScopeContractError.BusinessError.HierarchyViolation -> -32013 // Hierarchy violation
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> -32013 // Hierarchy violation (alias constraint)
        is ScopeContractError.BusinessError.AlreadyDeleted,
        is ScopeContractError.BusinessError.ArchivedScope -> -32014 // State conflict
        is ScopeContractError.BusinessError.HasChildren -> -32010 // Business constraint violation
        is ScopeContractError.SystemError -> -32000 // Server error
        else -> -32010 // Generic business error
    }

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
    }

    /**
     * Check if we have a cached result for this idempotent operation.
     * Returns the cached result if found and not expired, null otherwise.
     */
    private fun checkIdempotency(toolName: String, idempotencyKey: String, arguments: Map<String, kotlinx.serialization.json.JsonElement>): CallToolResult? {
        if (!idempotencyKey.matches(IDEMPOTENCY_KEY_PATTERN)) {
            return err("Invalid idempotency key format")
        }

        val cacheKey = buildCacheKey(toolName, idempotencyKey, arguments)
        val stored = idempotencyStore[cacheKey]

        return if (stored != null) {
            val now = Clock.System.now()
            val age = now - stored.timestamp

            if (age < idempotencyTtlMinutes.minutes) {
                // Return cached result
                stored.result
            } else {
                // Expired, remove it
                idempotencyStore.remove(cacheKey)
                null
            }
        } else {
            null
        }
    }

    /**
     * Store the result of an idempotent operation.
     */
    private fun storeIdempotency(toolName: String, idempotencyKey: String, arguments: Map<String, kotlinx.serialization.json.JsonElement>, result: CallToolResult) {
        val cacheKey = buildCacheKey(toolName, idempotencyKey, arguments)
        val stored = StoredResult(result, Clock.System.now())
        idempotencyStore[cacheKey] = stored

        // Clean up old entries (simple TTL-based cleanup)
        cleanupExpiredEntries()
    }

    /**
     * Build a cache key from tool name, idempotency key, and normalized arguments.
     */
    private fun buildCacheKey(toolName: String, idempotencyKey: String, arguments: Map<String, kotlinx.serialization.json.JsonElement>): String {
        // Normalize arguments by removing null values and sorting keys
        val normalized = arguments
            .filterValues { it !is kotlinx.serialization.json.JsonNull }
            .toSortedMap()
            .toString()

        // Simple hash using Kotlin's hashCode
        val argsHash = normalized.hashCode().toString(16)

        return "$toolName|$idempotencyKey|$argsHash"
    }

    /**
     * Clean up expired entries from the idempotency store.
     * This is a simple implementation - in production, you might want a more sophisticated approach.
     */
    private fun cleanupExpiredEntries() {
        val now = Clock.System.now()
        val cutoff = now - idempotencyTtlMinutes.minutes

        idempotencyStore.entries.removeIf { (_, stored) ->
            stored.timestamp < cutoff
        }
    }
}
