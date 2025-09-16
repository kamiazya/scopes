package io.github.kamiazya.scopes.interfaces.mcp.tools.handlers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.platform.observability.logging.LogLevel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class AliasesSetCanonicalCamelToolHandlerTest : StringSpec({

    fun createMockLogger(): Logger = object : Logger {
        override fun debug(message: String, context: Map<String, Any>) {}
        override fun info(message: String, context: Map<String, Any>) {}
        override fun warn(message: String, context: Map<String, Any>) {}
        override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {}
        override fun isEnabledFor(level: LogLevel) = true
        override fun withContext(context: Map<String, Any>) = this
        override fun withName(name: String) = this
    }

    fun createMockScope(id: String, alias: String, title: String): ScopeResult =
        ScopeResult(
            id = id,
            title = title,
            description = null,
            parentId = null,
            canonicalAlias = alias,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            isArchived = false,
            aspects = emptyMap()
        )

    "tool has correct name and annotations" {
        val handler = AliasesSetCanonicalCamelToolHandler()
        
        handler.name shouldBe "aliases.setCanonical"
        handler.description shouldBe "Set canonical alias"
        
        val annotations = handler.annotations.shouldNotBeNull()
        annotations.readOnlyHint shouldBe false
        annotations.destructiveHint shouldBe true
        annotations.idempotentHint shouldBe false
    }

    "sets canonical alias successfully" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        val scope = createMockScope("scope-id", "current-alias", "Test Scope")
        
        coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("current-alias")) } returns Either.Right(scope)
        coEvery { mockCommand.setCanonicalAlias(any()) } returns Either.Right(Unit)
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "current-alias")
                put("newCanonicalAlias", "new-canonical-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
        )
        
        val result = runBlocking { handler.handle(context) }
        
        result.isError shouldBe false
        result.content.size shouldBe 1
        val content = result.content[0] as TextContent
        val json = Json.parseToJsonElement(content.text ?: "").jsonObject
        
        (json["scopeAlias"]?.jsonPrimitive?.content ?: "") shouldBe "current-alias"
        (json["newCanonicalAlias"]?.jsonPrimitive?.content ?: "") shouldBe "new-canonical-alias"
        
        coVerify {
            mockCommand.setCanonicalAlias(
                SetCanonicalAliasCommand(
                    scopeId = "scope-id",
                    aliasName = "new-canonical-alias"
                )
            )
        }
    }

    "handles missing scopeAlias parameter" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("newCanonicalAlias", "new-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            runBlocking { handler.handle(context) }
        }
        
        exception.message shouldContain "Missing required parameter: scopeAlias"
    }

    "handles missing newCanonicalAlias parameter" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "current-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            runBlocking { handler.handle(context) }
        }
        
        exception.message shouldContain "Missing required parameter: newCanonicalAlias"
    }

    "handles scope not found error" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("non-existent")) } returns 
            Either.Left(ScopeContractError.BusinessError.AliasNotFound(alias = "non-existent"))
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "non-existent")
                put("newCanonicalAlias", "new-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
        )
        
        val result = runBlocking { handler.handle(context) }
        
        result.isError shouldBe true
        result.content.size shouldBe 1
        val content = result.content[0] as TextContent
        (content.text ?: "") shouldContain "non-existent"
    }

    "handles alias not found error" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        val scope = createMockScope("scope-id", "current-alias", "Test Scope")
        
        coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("current-alias")) } returns Either.Right(scope)
        coEvery { mockCommand.setCanonicalAlias(any()) } returns 
            Either.Left(ScopeContractError.BusinessError.AliasNotFound(alias = "non-existent-new-alias"))
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "current-alias")
                put("newCanonicalAlias", "non-existent-new-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
        )
        
        val result = runBlocking { handler.handle(context) }
        
        result.isError shouldBe true
        result.content.size shouldBe 1
        val content = result.content[0] as TextContent
        (content.text ?: "") shouldContain "non-existent-new-alias"
    }

    "handles idempotent requests" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val logger = createMockLogger()
        
        val scope = createMockScope("scope-id", "current-alias", "Test Scope")
        
        coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("current-alias")) } returns Either.Right(scope)
        coEvery { mockCommand.setCanonicalAlias(any()) } returns Either.Right(Unit)
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val services = Services(
            errors = createErrorMapper(),
            idempotency = createIdempotencyService(createArgumentCodec()),
            codec = createArgumentCodec(),
            logger = logger,
        )
        
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "current-alias")
                put("newCanonicalAlias", "new-canonical-alias")
                put("idempotencyKey", "test-key-123")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = services
        )
        
        // First request
        val result1 = runBlocking { handler.handle(context) }
        
        // Second request with same idempotency key
        val result2 = runBlocking { handler.handle(context) }
        
        // Should return the same result
        result1.content[0].toString() shouldBe result2.content[0].toString()
        
        // Query and command should only be called once
        coVerify(exactly = 1) { mockQuery.getScopeByAlias(any()) }
        coVerify(exactly = 1) { mockCommand.setCanonicalAlias(any()) }
    }

    "logs debug information" {
        val mockQuery = mockk<ScopeManagementQueryPort>()
        val mockCommand = mockk<ScopeManagementCommandPort>()
        val mockLogger = mockk<Logger>(relaxed = true)
        
        every { mockLogger.isEnabledFor(any()) } returns true
        every { mockLogger.withContext(any()) } returns mockLogger
        every { mockLogger.withName(any()) } returns mockLogger
        
        val scope = createMockScope("scope-id", "current-alias", "Test Scope")
        
        coEvery { mockQuery.getScopeByAlias(GetScopeByAliasQuery("current-alias")) } returns Either.Right(scope)
        coEvery { mockCommand.setCanonicalAlias(any()) } returns Either.Right(Unit)
        
        val handler = AliasesSetCanonicalCamelToolHandler()
        val context = ToolContext(
            args = buildJsonObject {
                put("scopeAlias", "current-alias")
                put("newCanonicalAlias", "new-canonical-alias")
            },
            ports = Ports(query = mockQuery, command = mockCommand),
            services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = mockLogger,
            )
        )
        
        runBlocking { handler.handle(context) }
        
        verify { mockLogger.debug(match { it.contains("Setting canonical alias") }, any()) }
    }
})