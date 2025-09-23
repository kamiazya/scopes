package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class ScopeCreateToolHandlerTest :
    StringSpec({

        fun createMockLogger(): Logger = mockk<Logger>(relaxed = true)

        "tool has correct name and annotations" {
            val handler = ScopeCreateToolHandler()

            handler.name shouldBe "scopes.create"
            handler.description shouldBe "Create a new scope. Parent can be specified by alias."

            val annotations = handler.annotations.shouldNotBeNull()
            annotations.readOnlyHint shouldBe false
            annotations.destructiveHint shouldBe true
            annotations.idempotentHint shouldBe false
        }

        "creates scope with required parameters" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "Test Scope",
                description = null,
                parentId = null,
                canonicalAlias = "test-scope-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Test Scope")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                ),
            )

            val result = runBlocking { handler.handle(context) }

            result.content.size shouldBe 1
            val content = result.content[0] as TextContent
            val json = Json.parseToJsonElement(content.text ?: "").jsonObject

            (json["canonicalAlias"]?.jsonPrimitive?.content ?: "") shouldBe "test-scope-1"
            (json["title"]?.jsonPrimitive?.content ?: "") shouldBe "Test Scope"

            coVerify {
                mockCommand.createScope(
                    CreateScopeCommand.WithAutoAlias(
                        title = "Test Scope",
                        description = null,
                        parentId = null,
                    ),
                )
            }
        }

        "creates scope with parent" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val parentScope = ScopeResult(
                id = "parent-id",
                title = "Parent Scope",
                description = null,
                parentId = null,
                canonicalAlias = "parent-scope",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                isArchived = false,
                aspects = emptyMap(),
            )

            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "Child Scope",
                description = "A child scope",
                parentId = "parent-id",
                canonicalAlias = "child-scope-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("parent-scope")) } returns Either.Right(parentScope)
            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Child Scope")
                    put("description", "A child scope")
                    put("parentAlias", "parent-scope")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                ),
            )

            val result = runBlocking { handler.handle(context) }

            result.content.size shouldBe 1
            val content = result.content[0] as TextContent
            val json = Json.parseToJsonElement(content.text ?: "").jsonObject

            (json["canonicalAlias"]?.jsonPrimitive?.content ?: "") shouldBe "child-scope-1"
            (json["title"]?.jsonPrimitive?.content ?: "") shouldBe "Child Scope"
            (json["description"]?.jsonPrimitive?.content ?: "") shouldBe "A child scope"
            (json["parentAlias"]?.jsonPrimitive?.content ?: "") shouldBe "parent-scope"

            coVerify {
                mockCommand.createScope(
                    CreateScopeCommand.WithAutoAlias(
                        title = "Child Scope",
                        description = "A child scope",
                        parentId = "parent-id",
                    ),
                )
            }
        }

        "creates scope with custom alias" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "Custom Alias Scope",
                description = null,
                parentId = null,
                canonicalAlias = "my-custom-alias",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Custom Alias Scope")
                    put("customAlias", "my-custom-alias")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                ),
            )

            val result = runBlocking { handler.handle(context) }

            result.content.size shouldBe 1
            val content = result.content[0] as TextContent
            val json = Json.parseToJsonElement(content.text ?: "").jsonObject

            (json["canonicalAlias"]?.jsonPrimitive?.content ?: "") shouldBe "my-custom-alias"

            coVerify {
                mockCommand.createScope(
                    CreateScopeCommand.WithCustomAlias(
                        title = "Custom Alias Scope",
                        description = null,
                        parentId = null,
                        alias = "my-custom-alias",
                    ),
                )
            }
        }

        "handles missing title parameter" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    // Missing title
                    put("description", "Some description")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                ),
            )

            val exception = shouldThrow<IllegalArgumentException> {
                runBlocking { handler.handle(context) }
            }

            exception.message shouldContain "Missing required parameter: title"
        }

        "handles parent not found error" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("non-existent")) } returns
                Either.Left(ScopeContractError.BusinessError.AliasNotFound(alias = "non-existent"))

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Test Scope")
                    put("parentAlias", "non-existent")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                ),
            )

            val result = runBlocking { handler.handle(context) }

            result.isError shouldBe true
            result.content.size shouldBe 1
            val content = result.content[0] as TextContent
            content.text shouldContain "non-existent"
        }

        "handles idempotent requests" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "Test Scope",
                description = null,
                parentId = null,
                canonicalAlias = "test-scope-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val handler = ScopeCreateToolHandler()
            val services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )

            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Test Scope")
                    put("idempotencyKey", "test-key-123")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = services,
            )

            // First request
            val result1 = runBlocking { handler.handle(context) }

            // Second request with same idempotency key
            val result2 = runBlocking { handler.handle(context) }

            // Should return the same result
            result1.content[0].toString() shouldBe result2.content[0].toString()

            // Command should only be called once
            coVerify(exactly = 1) { mockCommand.createScope(any()) }
        }

        "logs debug information" {
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val mockLogger = mockk<Logger>(relaxed = true)

            every { mockLogger.isEnabledFor(any()) } returns true
            every { mockLogger.withContext(any()) } returns mockLogger
            every { mockLogger.withName(any()) } returns mockLogger

            val createdScope = CreateScopeResult(
                id = "new-scope-id",
                title = "Test Scope",
                description = null,
                parentId = null,
                canonicalAlias = "test-scope-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            )

            coEvery { mockCommand.createScope(any()) } returns Either.Right(createdScope)

            val handler = ScopeCreateToolHandler()
            val context = ToolContext(
                args = buildJsonObject {
                    put("title", "Test Scope")
                },
                ports = Ports(query = mockQuery, command = mockCommand),
                services = Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = mockLogger,
                ),
            )

            runBlocking { handler.handle(context) }

            verify { mockLogger.debug(match { it.contains("Creating scope: Test Scope") }, any()) }
        }
    })
