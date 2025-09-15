package io.github.kamiazya.scopes.interfaces.mcp.adapters

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.InMemoryTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

/**
 * Round-trip E2E tests using InMemoryTransport to test the actual MCP Server implementation.
 * 
 * These tests start a real server with InMemoryTransport and use a Client to perform
 * actual tool calls, ensuring the complete MCP tool integration works end-to-end.
 */
class McpRoundTripTest : BaseIntegrationTest() {

    init {
        "should perform successful create -> get round trip" {
            runTest {
                setupMocks()

                // Setup successful create response
                val createdScope = TestData.createScopeResult(
                    canonicalAlias = "test-scope",
                    title = "Test Scope"
                )
                MockConfig.run { 
                    commandPort.mockSuccessfulCreate(createdScope)
                    queryPort.mockSuccessfulGet("test-scope", createdScope)
                }

                // Create server and client with InMemoryTransport
                val adapter = McpServerAdapter(scopeQueryPort, scopeCommandPort, logger)
                val server = adapter.createServer()
                val transport = InMemoryTransport()
                val client = Client()

                // Connect server and client
                server.connect(transport.serverTransport)
                client.connect(transport.clientTransport)

                try {
                    // Step 1: Create a scope
                    val createArgs = buildJsonObject {
                        put("title", "Test Scope")
                        put("idempotencyKey", "test-key-123")
                    }

                    val createResult = client.callTool("scopes.create", createArgs)
                    createResult.isError shouldBe false
                    createResult.content.first().text shouldContain "test-scope"

                    // Step 2: Get the created scope
                    val getArgs = buildJsonObject {
                        put("alias", "test-scope")
                    }

                    val getResult = client.callTool("scopes.get", getArgs)
                    getResult.isError shouldBe false
                    getResult.content.first().text shouldContain "Test Scope"

                } finally {
                    client.close()
                    server.close()
                }
            }
        }

        "should handle error case - aliases.remove canonical alias" {
            runTest {
                setupMocks()

                // Setup error case for removing canonical alias
                MockConfig.run { 
                    commandPort.mockCannotRemoveCanonicalAlias("main-scope")
                }

                // Create server and client with InMemoryTransport
                val adapter = McpServerAdapter(scopeQueryPort, scopeCommandPort, logger)
                val server = adapter.createServer()
                val transport = InMemoryTransport()
                val client = Client()

                // Connect server and client
                server.connect(transport.serverTransport)
                client.connect(transport.clientTransport)

                try {
                    // Try to remove canonical alias (should fail)
                    val removeArgs = buildJsonObject {
                        put("scopeAlias", "main-scope")
                        put("aliasToRemove", "main-scope") // trying to remove canonical
                    }

                    val removeResult = client.callTool("aliases.remove", removeArgs)
                    removeResult.isError shouldBe true
                    removeResult.content.first().text shouldContain "CannotRemoveCanonicalAlias"

                } finally {
                    client.close()
                    server.close()
                }
            }
        }

        "should verify idempotency - same key returns same result" {
            runTest {
                setupMocks()

                // Setup successful create response
                val createdScope = TestData.createScopeResult(
                    canonicalAlias = "idempotent-scope",
                    title = "Idempotent Test"
                )
                MockConfig.run { 
                    commandPort.mockSuccessfulCreate(createdScope)
                }

                // Create server and client with InMemoryTransport
                val adapter = McpServerAdapter(scopeQueryPort, scopeCommandPort, logger)
                val server = adapter.createServer()
                val transport = InMemoryTransport()
                val client = Client()

                // Connect server and client
                server.connect(transport.serverTransport)
                client.connect(transport.clientTransport)

                try {
                    val createArgs = buildJsonObject {
                        put("title", "Idempotent Test")
                        put("idempotencyKey", "unique-key-456")
                    }

                    // First call
                    val firstResult = client.callTool("scopes.create", createArgs)
                    firstResult.isError shouldBe false
                    val firstContent = firstResult.content.first().text

                    // Second call with same idempotency key
                    val secondResult = client.callTool("scopes.create", createArgs)
                    secondResult.isError shouldBe false
                    val secondContent = secondResult.content.first().text

                    // Should return identical results
                    firstContent shouldBe secondContent

                } finally {
                    client.close()
                    server.close()
                }
            }
        }

        "should test aliases.resolve exact match behavior" {
            runTest {
                setupMocks()

                // Setup successful resolve response
                val resolvedScope = TestData.createScopeResult(
                    canonicalAlias = "project-alpha",
                    title = "Project Alpha"
                )
                MockConfig.run { 
                    queryPort.mockSuccessfulGet("project-alpha", resolvedScope)
                }

                // Create server and client with InMemoryTransport
                val adapter = McpServerAdapter(scopeQueryPort, scopeCommandPort, logger)
                val server = adapter.createServer()
                val transport = InMemoryTransport()
                val client = Client()

                // Connect server and client
                server.connect(transport.serverTransport)
                client.connect(transport.clientTransport)

                try {
                    // Test exact match resolution
                    val resolveArgs = buildJsonObject {
                        put("alias", "project-alpha")
                    }

                    val resolveResult = client.callTool("aliases.resolve", resolveArgs)
                    resolveResult.isError shouldBe false
                    
                    val responseJson = Json.parseToJsonElement(resolveResult.content.first().text).jsonObject
                    responseJson["alias"]?.jsonPrimitive?.content shouldBe "project-alpha"
                    responseJson["canonicalAlias"]?.jsonPrimitive?.content shouldBe "project-alpha"
                    responseJson["title"]?.jsonPrimitive?.content shouldBe "Project Alpha"

                } finally {
                    client.close()
                    server.close()
                }
            }
        }

        "should test aliases.resolve not found behavior" {
            runTest {
                setupMocks()

                // Setup not found response
                MockConfig.run { 
                    queryPort.mockNotFound("non-existent")
                }

                // Create server and client with InMemoryTransport
                val adapter = McpServerAdapter(scopeQueryPort, scopeCommandPort, logger)
                val server = adapter.createServer()
                val transport = InMemoryTransport()
                val client = Client()

                // Connect server and client
                server.connect(transport.serverTransport)
                client.connect(transport.clientTransport)

                try {
                    // Test not found case
                    val resolveArgs = buildJsonObject {
                        put("alias", "non-existent")
                    }

                    val resolveResult = client.callTool("aliases.resolve", resolveArgs)
                    resolveResult.isError shouldBe true
                    resolveResult.content.first().text shouldContain "AliasNotFound"

                } finally {
                    client.close()
                    server.close()
                }
            }
        }
    }
}