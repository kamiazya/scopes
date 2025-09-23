package io.github.kamiazya.scopes.interfaces.mcp.integration

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.*
import io.github.kamiazya.scopes.contracts.scopemanagement.results.*
import io.github.kamiazya.scopes.interfaces.mcp.server.ToolRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.support.*
import io.github.kamiazya.scopes.interfaces.mcp.tools.*
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.*
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class McpServerIntegrationTest :
    StringSpec({

        fun createMockLogger(): Logger = mockk<Logger>(relaxed = true)

        fun createMockScope(id: String, alias: String, title: String, parentId: String? = null): ScopeResult = ScopeResult(
            id = id,
            title = title,
            description = null,
            parentId = parentId,
            canonicalAlias = alias,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            isArchived = false,
            aspects = emptyMap(),
        )

        "ToolRegistrar registers all tool handlers" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()
            val mockServer = mockk<Server>(relaxed = true)

            val ports = Ports(query = mockQuery, command = mockCommand)
            val services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )

            // Create tool handlers
            val handlers = listOf(
                ScopeGetToolHandler(),
                ScopeCreateToolHandler(),
                ScopeUpdateToolHandler(),
                ScopeDeleteToolHandler(),
                ScopeChildrenToolHandler(),
                ScopesRootsToolHandler(),
                ScopesListAliasesToolHandler(),
                AliasesSetCanonicalCamelToolHandler(),
                AliasesAddToolHandler(),
                AliasesRemoveToolHandler(),
                AliasResolveToolHandler(),
            )

            // Register tools
            val toolRegistrar = ToolRegistrar(handlers) { ports to services }
            toolRegistrar.register(mockServer)

            // Verify that addTool was called for each handler
            verify(atLeast = 11) { mockServer.addTool(any(), any(), any(), any(), any(), any(), any()) }
        }

        "ToolRegistrar handles tool execution through context" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val scope = createMockScope("scope-id", "test-alias", "Test Scope")

            coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("test-alias")) } returns Either.Right(scope)

            val ports = Ports(query = mockQuery, command = mockCommand)
            val services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )

            // Test tool handler directly
            val handler = ScopeGetToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("alias", "test-alias")
                },
                ports = ports,
                services = services,
            )

            val result = runBlocking { handler.handle(context) }

            result.isError shouldBe false
            result.content.size shouldBe 1
            val content = result.content[0] as TextContent
            (content.text ?: "") shouldContain "test-alias"
            (content.text ?: "") shouldContain "Test Scope"

            coVerify { mockQuery.getScopeByAlias(GetScopeByAliasQuery("test-alias")) }
        }

        "Tool handlers work with proper context and services" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            // Setup mock response for create
            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "New Feature",
                description = "Test description",
                parentId = null,
                canonicalAlias = "feature-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val ports = Ports(query = mockQuery, command = mockCommand)
            val services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )

            // Test create handler
            val createHandler = ScopeCreateToolHandler()
            val createContext = ToolContext(
                args = buildJsonObject {
                    put("title", "New Feature")
                    put("description", "Test description")
                },
                ports = ports,
                services = services,
            )

            val createResult = runBlocking { createHandler.handle(createContext) }

            createResult.isError shouldBe false
            val content = createResult.content[0] as TextContent
            (content.text ?: "") shouldContain "feature-1"
            (content.text ?: "") shouldContain "New Feature"

            coVerify {
                mockCommand.createScope(
                    CreateScopeCommand.WithAutoAlias(
                        title = "New Feature",
                        description = "Test description",
                        parentId = null,
                    ),
                )
            }
        }
    })
