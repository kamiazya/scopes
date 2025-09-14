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
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.time.Duration.Companion.minutes
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import io.github.kamiazya.scopes.platform.observability.logging.Logger

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
    private val scopeCommandPort: ScopeManagementCommandPort,
    private val logger: Logger
) {
    // Idempotency store: toolName|idempotencyKey|argsHash -> StoredResult
    private val idempotencyStore: MutableMap<String, StoredResult> = ConcurrentHashMap()
    private val idempotencyTtlMinutes = 10L
    private var stdioOut: java.io.PrintStream? = null

    companion object {
        private val IDEMPOTENCY_KEY_PATTERN = Regex("^[A-Za-z0-9_-]{8,128}$")
    }

    fun notifyToolsListChanged(): Boolean = sendJsonRpcNotification("tools/list_changed")

    fun notifyResourcesListChanged(): Boolean = sendJsonRpcNotification("resources/list_changed")

    fun notifyPromptsListChanged(): Boolean = sendJsonRpcNotification("prompts/list_changed")

    /**
     * Run the MCP server on stdio transport.
     * This is the main entry point for the MCP server.
     */
    fun runStdio(
        inputStream: java.io.InputStream = System.`in`,
        outputStream: java.io.OutputStream = System.out
    ) {
        val server = createServer()
        val ps = java.io.PrintStream(outputStream, true)
        val transport = StdioServerTransport(
            inputStream = inputStream.asSource().buffered(),
            outputStream = outputStream.asSink().buffered()
        )

        runBlocking {
            try {
                logger.info("Starting MCP server (stdio)")
                stdioOut = ps
                server.connect(transport)
                logger.info("MCP server connected successfully")

                // Keep process alive; MCP client typically terminates the process when done.
                try {
                    while (true) kotlinx.coroutines.delay(60_000)
                } catch (_: Throwable) {
                    // exit gracefully
                }
            } catch (e: Exception) {
                logger.error("Failed to start MCP server", throwable = e)
                throw e
            }
        }
    }

    private fun sendJsonRpcNotification(method: String, params: JsonObject = buildJsonObject { }): Boolean {
        return try {
            val out = stdioOut
            if (out == null) {
                logger.warn("Cannot send notification: stdioOut is null", mapOf("method" to method))
                false
            } else {
                val json = buildJsonObject {
                    put("jsonrpc", "2.0")
                    put("method", method)
                    put("params", params)
                }.toString()
                out.println(json)
                out.flush()
                logger.debug("Sent JSON-RPC notification", mapOf("method" to method))
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to send JSON-RPC notification", mapOf("method" to method), e)
            false
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
                    resources = ServerCapabilities.Resources(subscribe = false, listChanged = true),
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
                    resources = ServerCapabilities.Resources(subscribe = false, listChanged = true),
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("alias") { put("type", "string") }
                        putJsonObject("title") { put("type", "string") }
                    }
                    putJsonArray("required") { add("canonicalAlias"); add("title") }
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
                        put("canonicalAlias", scope.canonicalAlias)
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
            ),
            outputSchema = Tool.Output(
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
                    putJsonArray("required") { add("canonicalAlias"); add("title"); add("createdAt"); add("updatedAt") }
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
            ),
            outputSchema = Tool.Output(
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
                    putJsonArray("required") { add("canonicalAlias"); add("title"); add("createdAt") }
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("title") { put("type", "string") }
                        putJsonObject("description") { put("type", "string") }
                        putJsonObject("updatedAt") { put("type", "string") }
                    }
                    putJsonArray("required") { add("canonicalAlias"); add("title"); add("updatedAt") }
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val title = req.arguments["title"]?.jsonPrimitive?.content
            val description = req.arguments["description"]?.jsonPrimitive?.content
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Idempotency check
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("scopes.update", key, req.arguments)
                if (cached != null) return@addTool cached
            }

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            val (scopeId, canonicalAlias) = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id to scopeResult.value.canonicalAlias
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

            val toolResult = result.fold(
                { error -> errorResult(error) },
                { updated ->
                    val json = buildJsonObject {
                        put("canonicalAlias", canonicalAlias)
                        put("title", updated.title)
                        updated.description?.let { put("description", it) }
                        put("updatedAt", updated.updatedAt.toString())
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )

            // Store for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("scopes.update", key, req.arguments, toolResult)
            }

            toolResult
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("deleted") { put("type", "boolean") }
                    }
                    putJsonArray("required") { add("canonicalAlias"); add("deleted") }
                }
            )
        ) { req ->
            val alias = req.arguments["alias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'alias'")
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Idempotency check
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("scopes.delete", key, req.arguments)
                if (cached != null) return@addTool cached
            }

            val scopeResult = runBlocking {
                scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias))
            }
            val (scopeId, canonicalAlias) = when (scopeResult) {
                is Either.Left -> return@addTool errorResult(scopeResult.value)
                is Either.Right -> scopeResult.value.id to scopeResult.value.canonicalAlias
            }

            val result = runBlocking {
                scopeCommandPort.deleteScope(DeleteScopeCommand(id = scopeId))
            }

            val toolResult = result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("canonicalAlias", canonicalAlias)
                        put("deleted", true)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )

            // Store for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("scopes.delete", key, req.arguments, toolResult)
            }

            toolResult
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
            ),
            outputSchema = Tool.Output(
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
                                putJsonArray("required") { add("canonicalAlias"); add("title") }
                            }
                        }
                    }
                    putJsonArray("required") { add("parentAlias"); add("children") }
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
            ),
            outputSchema = Tool.Output(
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
                                putJsonArray("required") { add("canonicalAlias"); add("title") }
                            }
                        }
                    }
                    putJsonArray("required") { add("roots") }
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
        // debug.listChanged - development-only helper to trigger list_changed notifications
        server.addTool(
            name = "debug.listChanged",
            description = "Trigger list_changed notifications for tools/resources/prompts (debug only)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonArray("required") { add("target") }
                    putJsonObject("properties") {
                        putJsonObject("target") {
                            put("type", "string")
                            putJsonArray("enum") { add("tools"); add("resources"); add("prompts") }
                        }
                    }
                }
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("ok") { put("type", "boolean") }
                        putJsonObject("target") { put("type", "string") }
                    }
                    putJsonArray("required") { add("ok"); add("target") }
                }
            )
        ) { req ->
            val target = req.arguments["target"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'target'")
            val ok = when (target) {
                "tools" -> notifyToolsListChanged()
                "resources" -> notifyResourcesListChanged()
                "prompts" -> notifyPromptsListChanged()
                else -> false
            }
            val json = buildJsonObject {
                put("ok", ok)
                put("target", target)
            }
            CallToolResult(content = listOf(TextContent(json.toString())))
        }
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("addedAlias") { put("type", "string") }
                    }
                    putJsonArray("required") { add("scopeAlias"); add("addedAlias") }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val newAlias = req.arguments["newAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'newAlias'")
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Idempotency check
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("aliases.add", key, req.arguments)
                if (cached != null) return@addTool cached
            }

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

            val toolResult = result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("addedAlias", newAlias)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )

            // Store for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("aliases.add", key, req.arguments, toolResult)
            }

            toolResult
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("removedAlias") { put("type", "string") }
                    }
                    putJsonArray("required") { add("scopeAlias"); add("removedAlias") }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val aliasToRemove = req.arguments["aliasToRemove"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'aliasToRemove'")
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Idempotency check
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("aliases.remove", key, req.arguments)
                if (cached != null) return@addTool cached
            }

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

            val toolResult = result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("removedAlias", aliasToRemove)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )

            // Store for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("aliases.remove", key, req.arguments, toolResult)
            }

            toolResult
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("newCanonicalAlias") { put("type", "string") }
                    }
                    putJsonArray("required") { add("scopeAlias"); add("newCanonicalAlias") }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val newCanonicalAlias = req.arguments["newCanonicalAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'newCanonicalAlias'")
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            // Idempotency check
            idempotencyKey?.let { key ->
                val cached = checkIdempotency("aliases.setCanonical", key, req.arguments)
                if (cached != null) return@addTool cached
            }

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

            val toolResult = result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("newCanonicalAlias", newCanonicalAlias)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
            // Store for idempotency
            idempotencyKey?.let { key ->
                storeIdempotency("aliases.setCanonical", key, req.arguments, toolResult)
            }

            toolResult
        }

        // Backward-compat snake_case alias for setCanonical
        server.addTool(
            name = "aliases.set_canonical",
            description = "[Deprecated] Set canonical alias (snake_case name)",
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("newCanonicalAlias") { put("type", "string") }
                    }
                    putJsonArray("required") { add("scopeAlias"); add("newCanonicalAlias") }
                }
            )
        ) { req ->
            val scopeAlias = req.arguments["scopeAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'scopeAlias'")
            val newCanonicalAlias = req.arguments["newCanonicalAlias"]?.jsonPrimitive?.content ?: return@addTool err("Missing 'newCanonicalAlias'")
            val idempotencyKey = req.arguments["idempotencyKey"]?.jsonPrimitive?.content

            idempotencyKey?.let { key ->
                val cached = checkIdempotency("aliases.setCanonical", key, req.arguments)
                if (cached != null) return@addTool cached
            }

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

            val toolResult = result.fold(
                { error -> errorResult(error) },
                {
                    val json = buildJsonObject {
                        put("scopeAlias", scopeAlias)
                        put("newCanonicalAlias", newCanonicalAlias)
                    }
                    CallToolResult(content = listOf(TextContent(json.toString())))
                }
            )
            idempotencyKey?.let { key ->
                storeIdempotency("aliases.setCanonical", key, req.arguments, toolResult)
            }
            toolResult
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
            ),
            outputSchema = Tool.Output(
                properties = buildJsonObject {
                    put("type", "object")
                    put("additionalProperties", false)
                    putJsonObject("properties") {
                        putJsonObject("scopeAlias") { put("type", "string") }
                        putJsonObject("canonicalAlias") { put("type", "string") }
                        putJsonObject("aliases") {
                            put("type", "array")
                            putJsonObject("items") {
                                put("type", "object")
                                put("additionalProperties", false)
                                putJsonObject("properties") {
                                    putJsonObject("aliasName") { put("type", "string") }
                                    putJsonObject("isCanonical") { put("type", "boolean") }
                                    putJsonObject("aliasType") { put("type", "string") }
                                }
                                putJsonArray("required") { add("aliasName"); add("isCanonical") }
                            }
                        }
                    }
                    putJsonArray("required") { add("scopeAlias"); add("aliases") }
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
            val text = "See repository docs/reference/cli-quick-reference.md"
            val etag = computeEtag(text)
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(
                        text = text,
                        uri = it.uri,
                        mimeType = "text/markdown; charset=utf-8"
                    )
                )
            )
        }

        // Template-like resource for scope details by canonical alias (best-effort)
        // Note: Depending on SDK support, this may be treated as a static entry in resources/list.
        server.addResource(
            uri = "scopes:/scope/{canonicalAlias}",
            name = "Scope Details (JSON)",
            description = "Scope details by canonical alias using the standard object shape",
            mimeType = "application/json; charset=utf-8"
        ) { req ->
            val uri = req.uri
            val prefix = "scopes:/scope/"
            val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

            if (alias.isBlank()) {
                val payload = buildJsonObject {
                    put("error", buildJsonObject {
                        put("code", -32602)
                        put("message", "Missing or invalid alias in resource URI")
                    })
                }.toString()
                val etag = computeEtag(payload)
                return@addResource ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = payload,
                            uri = uri,
                            mimeType = "application/json; charset=utf-8"
                        )
                    )
                )
            }

            val res = runBlocking { scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias)) }
            res.fold(
                { error ->
                    val payload = buildJsonObject {
                        put("error", buildJsonObject {
                            put("code", getErrorCode(error))
                            put("message", mapContractError(error))
                        })
                    }.toString()
                    val etag = computeEtag(payload)
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = payload,
                                uri = uri,
                                mimeType = "application/json; charset=utf-8"
                            )
                        )
                    )
                },
                { scope ->
                    val payload = buildJsonObject {
                        put("canonicalAlias", scope.canonicalAlias)
                        put("title", scope.title)
                        scope.description?.let { put("description", it) }
                        put("createdAt", scope.createdAt.toString())
                        put("updatedAt", scope.updatedAt.toString())
                        putJsonArray("links") {
                            add(buildJsonObject { put("rel", "self"); put("uri", "scopes:/scope/${scope.canonicalAlias}") })
                            add(buildJsonObject { put("rel", "tree"); put("uri", "scopes:/tree/${scope.canonicalAlias}") })
                            add(buildJsonObject { put("rel", "tree.md"); put("uri", "scopes:/tree.md/${scope.canonicalAlias}") })
                        }
                    }.toString()
                    val etag = computeEtag(payload)
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = payload,
                                uri = uri,
                                mimeType = "application/json; charset=utf-8"
                            )
                        )
                    )
                }
            )
        }

        // Tree JSON (depth=1 for now)
        server.addResource(
            uri = "scopes:/tree/{canonicalAlias}",
            name = "Scope Tree (JSON)",
            description = "Scope with children (configurable depth).",
            mimeType = "application/json; charset=utf-8"
        ) { req ->
            val uri = req.uri
            val prefix = "scopes:/tree/"
            val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""
            // Optional query param: depth (default 1)
            val depth = runCatching {
                val qIndex = alias.indexOf('?')
                if (qIndex >= 0) alias.substring(qIndex + 1) else null
            }.getOrNull()
            val (pureAlias, depthValue) = run {
                val qPos = alias.indexOf('?')
                val a = if (qPos >= 0) alias.substring(0, qPos) else alias
                val d = depth?.split('=')?.let { if (it.size == 2 && it[0] == "depth") it[1].toIntOrNull() else null } ?: 1
                a to d.coerceIn(1, 5)
            }

            if (pureAlias.isBlank()) {
                return@addResource ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = buildJsonObject {
                                put("code", -32602)
                                putJsonObject("data") {
                                    put("type", "InvalidParams")
                                    put("message", "Missing or invalid alias in resource URI. Optional ?depth=1..5 supported.")
                                }
                            }.toString(),
                            uri = "scopes:/tree/$pureAlias?depth=$depthValue",
                            mimeType = "application/json; charset=utf-8"
                        )
                    )
                )
            }

            val rootRes = runBlocking { scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(pureAlias)) }
            rootRes.fold(
                { error ->
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = buildJsonObject {
                                    put("code", getErrorCode(error))
                                    putJsonObject("data") {
                                        put("type", error::class.simpleName)
                                        put("message", mapContractError(error))
                                    }
                                }.toString(),
                                uri = uri,
                                mimeType = "application/json; charset=utf-8"
                            )
                        )
                    )
                },
                { scope ->
                    // Build tree up to depthValue (limited to 1..5). For depth>1, recurse shallowly.
                    fun nodeJson(alias: String, currentDepth: Int): JsonObject {
                        val nodeRes = runBlocking { scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias)) }
                        return nodeRes.fold(
                            { _ -> buildJsonObject { put("canonicalAlias", alias); put("title", alias) } },
                            { s ->
                                val childrenJson = if (currentDepth < depthValue) {
                                    val chRes = runBlocking { scopeQueryPort.getChildren(GetChildrenQuery(parentId = s.id)) }
                                    chRes.fold(
                                        { emptyList<JsonObject>() },
                                        { ch -> ch.scopes.map { c ->
                                            nodeJson(c.canonicalAlias, currentDepth + 1)
                                        } }
                                    )
                                } else emptyList()
                                buildJsonObject {
                                    put("canonicalAlias", s.canonicalAlias)
                                    put("title", s.title)
                                    s.description?.let { put("description", it) }
                                    putJsonArray("children") { childrenJson.forEach { add(it) } }
                                    putJsonArray("links") {
                                        add(buildJsonObject { put("rel", "self"); put("uri", "scopes:/scope/${s.canonicalAlias}") })
                                    }
                                }
                            }
                        )
                    }
                    val json = nodeJson(scope.canonicalAlias, 1)
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = json.toString(),
                                uri = "scopes:/tree/${scope.canonicalAlias}?depth=$depthValue",
                                mimeType = "application/json; charset=utf-8"
                            )
                        )
                    )
                }
            )
        }

        // Tree Markdown (depth=1)
        server.addResource(
            uri = "scopes:/tree.md/{canonicalAlias}",
            name = "Scope Tree (Markdown)",
            description = "Scope with immediate children rendered as Markdown (depth=1)",
            mimeType = "text/markdown; charset=utf-8"
        ) { req ->
            val uri = req.uri
            val prefix = "scopes:/tree.md/"
            val alias = if (uri.startsWith(prefix)) uri.removePrefix(prefix) else ""

            if (alias.isBlank()) {
                return@addResource ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = "Invalid resource: missing alias",
                            uri = uri,
                            mimeType = "text/markdown; charset=utf-8"
                        )
                    )
                )
            }

            val rootRes = runBlocking { scopeQueryPort.getScopeByAlias(GetScopeByAliasQuery(alias)) }
            rootRes.fold(
                { error ->
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = "Error: ${mapContractError(error)}",
                                uri = uri,
                                mimeType = "text/markdown; charset=utf-8"
                            )
                        )
                    )
                },
                { scope ->
                    val childrenRes = runBlocking { scopeQueryPort.getChildren(GetChildrenQuery(parentId = scope.id)) }
                    val md = buildString {
                        appendLine("# ${scope.title} (${scope.canonicalAlias})")
                        scope.description?.let { appendLine("\n${it}") }
                        appendLine("\n## Children")
                        childrenRes.fold(
                            { _ -> appendLine("(no children or failed to load)") },
                            { children ->
                                if (children.scopes.isEmpty()) {
                                    appendLine("(no children)")
                                } else {
                                    children.scopes.forEach { ch ->
                                        appendLine("- ${ch.title} (${ch.canonicalAlias})" + (ch.description?.let { ": ${it}" } ?: ""))
                                    }
                                }
                            }
                        )
                        appendLine("\n[JSON] scopes:/tree/${scope.canonicalAlias}")
                    }
                    ReadResourceResult(
                        contents = listOf(
                            TextResourceContents(
                                text = md,
                                uri = uri,
                                mimeType = "text/markdown; charset=utf-8"
                            )
                        )
                    )
                }
            )
        }
    }

    private fun computeEtag(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { b -> "%02x".format(b) }
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

        // Outline prompt: produce bullet point outline for a scope
        server.addPrompt(
            name = "prompts.scopes.outline",
            description = "Create a concise bullet-point outline for a scope",
            arguments = listOf(
                PromptArgument(name = "alias", description = "Scope alias", required = true),
                PromptArgument(name = "depth", description = "Outline depth (1-3)", required = false)
            )
        ) { req ->
            val alias = req.arguments?.get("alias") ?: ""
            val depth = req.arguments?.get("depth") ?: "2"
            GetPromptResult(
                description = "Outline scope",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("You are a helpful assistant that produces crisp, structured outlines.")
                    ),
                    PromptMessage(
                        role = Role.user,
                        content = TextContent(
                            "Create a bullet-point outline (depth=$depth) for scope <alias>$alias</alias>.\n" +
                            "- Use short, informative bullets.\n" +
                            "- Group by sub-areas.\n" +
                            "- Do not invent facts beyond scope description and children."
                        )
                    )
                )
            )
        }

        // Planning prompt: propose next steps for a scope
        server.addPrompt(
            name = "prompts.scopes.plan",
            description = "Propose practical next steps for a scope",
            arguments = listOf(
                PromptArgument(name = "alias", description = "Scope alias", required = true),
                PromptArgument(name = "timeHorizon", description = "e.g. '1 week' or '1 month'", required = false)
            )
        ) { req ->
            val alias = req.arguments?.get("alias") ?: ""
            val horizon = req.arguments?.get("timeHorizon") ?: "2 weeks"
            GetPromptResult(
                description = "Plan next steps",
                messages = listOf(
                    PromptMessage(
                        role = Role.user,
                        content = TextContent("You write actionable plans that balance impact and effort.")
                    ),
                    PromptMessage(
                        role = Role.user,
                        content = TextContent(
                            "Given scope <alias>$alias</alias>, propose prioritized next steps for the next $horizon.\n" +
                            "- Each step should have an outcome and owner suggestion.\n" +
                            "- Keep it concise and concrete."
                        )
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
        val canonical = canonicalizeArguments(arguments)
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        val argsHash = digest.joinToString(separator = "") { b -> "%02x".format(b) }
        return "$toolName|$idempotencyKey|$argsHash"
    }

    private fun canonicalizeArguments(arguments: Map<String, JsonElement>): String {
        // remove nulls, sort keys at root, recursively canonicalize
        val filtered = buildJsonObject {
            arguments
                .filterValues { it !is JsonNull }
                .toSortedMap()
                .forEach { (k, v) -> put(k, v) }
        }
        return canonicalizeJson(filtered)
    }

    private fun canonicalizeJson(elem: JsonElement): String = when (elem) {
        is JsonNull -> "null"
        is JsonPrimitive -> if (elem.isString) {
            buildString {
                append('"')
                append(elem.content.replace("\\", "\\\\").replace("\"", "\\\""))
                append('"')
            }
        } else elem.toString()
        is JsonArray -> elem.joinToString(prefix = "[", postfix = "]") { canonicalizeJson(it) }
        is JsonObject -> {
            val entries = elem.entries.sortedBy { it.key }
            entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                val key = buildString {
                    append('"')
                    append(k.replace("\\", "\\\\").replace("\"", "\\\""))
                    append('"')
                }
                "$key:${canonicalizeJson(v)}"
            }
        }
        else -> elem.toString()
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
