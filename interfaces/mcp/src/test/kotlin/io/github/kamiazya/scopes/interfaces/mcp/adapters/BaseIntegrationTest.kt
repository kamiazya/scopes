package io.github.kamiazya.scopes.interfaces.mcp.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream

/**
 * Base class for MCP server integration tests using InMemoryTransport.
 * 
 * This class provides common infrastructure for testing MCP server functionality
 * including client-server communication setup, mock configuration, and assertion helpers.
 */
abstract class BaseIntegrationTest : StringSpec() {

    // Mock ports that can be configured in test subclasses
    protected lateinit var queryPort: ScopeManagementQueryPort
    protected lateinit var commandPort: ScopeManagementCommandPort
    protected lateinit var adapter: McpServerAdapter
    
    // MCP client-server infrastructure
    protected lateinit var client: Client
    protected lateinit var server: Server

    protected suspend fun setupClientServer() {
        queryPort = mockk(relaxed = true)
        commandPort = mockk(relaxed = true)
        adapter = McpServerAdapter(queryPort, commandPort)
        
        // Create the test server
        server = adapter.createTestServer()
        
        // Create connected pipes for stdio transport testing
        val serverToClient = PipedOutputStream()
        val clientFromServer = PipedInputStream(serverToClient)
        
        val clientToServer = PipedOutputStream()
        val serverFromClient = PipedInputStream(clientToServer)
        
        // Create transports using the connected pipes
        val serverTransport = StdioServerTransport(
            inputStream = BufferedInputStream(serverFromClient),
            outputStream = PrintStream(serverToClient)
        )
        
        val clientTransport = StdioClientTransport(
            input = clientFromServer,
            output = clientToServer
        )
        
        // Connect server and client using transports
        server.connect(serverTransport)
        client = Client(
            clientInfo = Implementation(name = "test-client", version = "1.0.0"),
        )
        client.connect(clientTransport)
    }
    
    protected fun setupMocks() {
        queryPort = mockk(relaxed = true)
        commandPort = mockk(relaxed = true)
        adapter = McpServerAdapter(queryPort, commandPort)
    }

    /**
     * Test data factory for creating standard test objects
     */
    protected object TestData {
        fun createScopeResult(
            id: String = "test-scope-id",
            canonicalAlias: String = "test-scope",
            title: String = "Test Scope",
            description: String? = null,
        ) = ScopeResult(
            id = id,
            canonicalAlias = canonicalAlias,
            title = title,
            description = description,
            parentId = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            isArchived = false
        )

        fun createCreateScopeResult(
            id: String = "new-scope-id",
            title: String = "New Scope",
            canonicalAlias: String = "new-scope",
            description: String? = null,
        ) = CreateScopeResult(
            id = id,
            title = title,
            description = description,
            parentId = null,
            canonicalAlias = canonicalAlias,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        fun createUpdateScopeResult(
            id: String = "updated-scope-id",
            title: String = "Updated Scope",
            description: String? = null,
            canonicalAlias: String = "updated-scope",
        ) = UpdateScopeResult(
            id = id,
            title = title,
            description = description,
            parentId = null,
            canonicalAlias = canonicalAlias,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }

    /**
     * Mock configuration helpers for common scenarios
     */
    protected object MockConfig {
        fun ScopeManagementQueryPort.mockSuccessfulGet(alias: String, result: ScopeResult) {
            coEvery { getScopeByAlias(GetScopeByAliasQuery(alias)) } returns result.right()
        }

        fun ScopeManagementQueryPort.mockNotFound(alias: String) {
            coEvery { getScopeByAlias(GetScopeByAliasQuery(alias)) } returns 
                ScopeContractError.BusinessError.AliasNotFound(alias).left()
        }

        fun ScopeManagementCommandPort.mockCreateSuccess(result: CreateScopeResult) {
            coEvery { createScope(any()) } returns result.right()
        }

        fun ScopeManagementCommandPort.mockCreateError(error: ScopeContractError) {
            coEvery { createScope(any()) } returns error.left()
        }

        fun ScopeManagementCommandPort.mockUpdateSuccess(result: UpdateScopeResult) {
            coEvery { updateScope(any()) } returns result.right()
        }

        fun ScopeManagementCommandPort.mockDeleteSuccess() {
            // Delete operations typically return Unit on success
            coEvery { deleteScope(any()) } returns Unit.right()
        }

        fun ScopeManagementCommandPort.mockDeleteError(error: ScopeContractError) {
            coEvery { deleteScope(any()) } returns error.left()
        }

        fun ScopeManagementCommandPort.mockCannotRemoveCanonicalAlias() {
            coEvery { removeAlias(any()) } returns 
                ScopeContractError.BusinessError.CannotRemoveCanonicalAlias.left()
        }
    }

    /**
     * Assertion helpers for MCP response validation
     */
    protected object Assertions {
        fun assertSuccessResponse(json: JsonObject): JsonObject {
            assert(!json.containsKey("error")) {
                "Expected success response but got error: $json"
            }
            return json
        }

        fun assertErrorResponse(json: JsonObject, expectedMessage: String? = null): JsonObject {
            assert(json.containsKey("error")) {
                "Expected error response but got success: $json"  
            }
            
            expectedMessage?.let { expected ->
                val errorField = json["error"]
                val actualMessage = when {
                    errorField?.jsonObject?.containsKey("message") == true -> 
                        errorField.jsonObject["message"]?.jsonPrimitive?.content
                    errorField?.jsonPrimitive != null -> 
                        errorField.jsonPrimitive.content
                    else -> null
                }
                assert(actualMessage?.contains(expected) == true) {
                    "Expected error message to contain '$expected' but got '$actualMessage'"
                }
            }
            return json
        }

        fun assertHasFields(json: JsonObject, vararg fields: String) {
            fields.forEach { field ->
                assert(json.containsKey(field)) {
                    "Expected field '$field' in response: $json"
                }
            }
        }

        fun assertFieldEquals(json: JsonObject, field: String, expected: String) {
            val actual = json[field]?.jsonPrimitive?.content
            assert(actual == expected) {
                "Expected field '$field' to be '$expected' but got '$actual'"
            }
        }
    }

    /**
     * Execute a tool call using real MCP client-server communication
     */
    protected suspend fun executeToolCall(
        toolName: String, 
        arguments: Map<String, Any?>
    ): JsonObject {
        return try {
            // Use real MCP client to call the tool
            val result = client.callTool(toolName, arguments)
            
            // Parse the JSON response - result can be null
            if (result == null) {
                return buildJsonObject { put("error", "Null result from tool call") }
            }
            
            when (result.content.size) {
                1 -> {
                    val content = result.content[0]
                    when (content) {
                        is TextContent -> 
                            Json.parseToJsonElement(content.text ?: "{}").jsonObject
                        else -> buildJsonObject { 
                            put("error", "Unsupported content type: ${content::class.simpleName}")
                        }
                    }
                }
                0 -> buildJsonObject { put("error", "Empty response") }
                else -> buildJsonObject { put("error", "Multiple response contents not supported") }
            }
        } catch (e: Exception) {
            buildJsonObject { 
                put("error", e.message ?: "Unknown error")
                put("exception", e::class.simpleName)
            }
        }
    }

    /**
     * Common test patterns that can be reused across different tool tests
     */
    protected fun testToolWithValidInput(
        toolName: String,
        arguments: Map<String, Any?>,
        setup: suspend () -> Unit = {},
        validation: (JsonObject) -> Unit = {}
    ) = toolName {
        runTest {
            setupClientServer()
            setup()
            
            val response = executeToolCall(toolName, arguments)
            Assertions.assertSuccessResponse(response)
            validation(response)
        }
    }

    protected fun testToolWithError(
        toolName: String,
        arguments: Map<String, Any?>,
        expectedErrorMessage: String? = null,
        setup: suspend () -> Unit = {}
    ) = "$toolName should return error" {
        runTest {
            setupClientServer()
            setup()
            
            val response = executeToolCall(toolName, arguments)
            Assertions.assertErrorResponse(response, expectedErrorMessage)
        }
    }
}