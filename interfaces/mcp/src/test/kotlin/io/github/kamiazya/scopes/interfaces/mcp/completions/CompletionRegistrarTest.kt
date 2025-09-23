package io.github.kamiazya.scopes.interfaces.mcp.completions

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.*
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.support.TestFixtures
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.CompleteRequest
import io.modelcontextprotocol.kotlin.sdk.CompleteResult
import io.modelcontextprotocol.kotlin.sdk.Method
import io.modelcontextprotocol.kotlin.sdk.PromptReference
import io.modelcontextprotocol.kotlin.sdk.ResourceTemplateReference
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class CompletionRegistrarTest :
    StringSpec({

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

        fun createMockLogger(): Logger = TestFixtures.mockLogger()

        "registers completion handler on server" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val registrar = CompletionRegistrar { Ports(query = mockQuery, command = mockCommand) to TestFixtures.services(logger) }

            registrar.register(server)

            verify(exactly = 1) {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    any(),
                )
            }
        }

        "completion handler provides alias completions for prompts" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val scope1 = createMockScope("id1", "proj-1", "Project One")
            val scope2 = createMockScope("id2", "proj-2", "Project Two")

            coEvery { mockQuery.getRootScopes(any()) } returns Either.Right(
                ScopeListResult(
                    scopes = listOf(scope1, scope2),
                    totalCount = 2,
                    offset = 0,
                    limit = 200,
                ),
            )

            coEvery { mockQuery.getChildren(any()) } returns Either.Right(
                ScopeListResult(
                    scopes = emptyList(),
                    totalCount = 0,
                    offset = 0,
                    limit = 200,
                ),
            )

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar { Ports(query = mockQuery, command = mockCommand) to TestFixtures.services(logger) }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = PromptReference(name = "prompts.scopes.plan"),
                argument = CompleteRequest.Argument(name = "alias", value = "proj"),
            )

            val result = runBlocking { capturedHandler(req, null) }

            result.completion.values shouldHaveSize 2
            result.completion.values shouldContain "proj-1"
            result.completion.values shouldContain "proj-2"
        }

        "completion handler provides timeHorizon completions" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar { Ports(query = mockQuery, command = mockCommand) to TestFixtures.services(logger) }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = PromptReference(name = "prompts.scopes.summarize"),
                argument = CompleteRequest.Argument(name = "timeHorizon", value = "1"),
            )

            val result = runBlocking { capturedHandler(req, null) }

            result.completion.values shouldHaveSize 2 // "1 week" and "1 month" match prefix "1"
            result.completion.values shouldContain "1 week"
            result.completion.values shouldContain "1 month"
        }

        "completion handler returns empty for unknown prompts" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar {
                Ports(query = mockQuery, command = mockCommand) to Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                )
            }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = PromptReference(name = "unknown.prompt"),
                argument = CompleteRequest.Argument(name = "alias", value = "test"),
            )

            val result = runBlocking { capturedHandler(req, null) }

            result.completion.values shouldHaveSize 0
            result.completion.hasMore shouldBe false
        }

        "completion handler provides depth completions for resource templates" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar {
                Ports(query = mockQuery, command = mockCommand) to Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                )
            }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = ResourceTemplateReference(uri = "scopes:/tree.md/"),
                argument = CompleteRequest.Argument(name = "depth", value = ""),
            )

            val result = runBlocking { capturedHandler(req, null) }

            result.completion.values shouldHaveSize 5
            result.completion.values shouldContain "1"
            result.completion.values shouldContain "5"
        }

        "completion handler finds aliases through BFS traversal" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val logger = createMockLogger()

            val root = createMockScope("root-id", "root", "Root")
            val child1 = createMockScope("child1-id", "child-1", "Child One", "root-id")
            val child2 = createMockScope("child2-id", "child-2", "Child Two", "root-id")

            coEvery { mockQuery.getRootScopes(any()) } returns Either.Right(
                ScopeListResult(
                    scopes = listOf(root),
                    totalCount = 1,
                    offset = 0,
                    limit = 200,
                ),
            )

            coEvery { mockQuery.getChildren(GetChildrenQuery(parentId = "root-id")) } returns Either.Right(
                ScopeListResult(
                    scopes = listOf(child1, child2),
                    totalCount = 2,
                    offset = 0,
                    limit = 200,
                ),
            )

            coEvery { mockQuery.getChildren(GetChildrenQuery(parentId = "child1-id")) } returns Either.Right(
                ScopeListResult(
                    scopes = emptyList(),
                    totalCount = 0,
                    offset = 0,
                    limit = 200,
                ),
            )

            coEvery { mockQuery.getChildren(GetChildrenQuery(parentId = "child2-id")) } returns Either.Right(
                ScopeListResult(
                    scopes = emptyList(),
                    totalCount = 0,
                    offset = 0,
                    limit = 200,
                ),
            )

            // Add mock for listAliases calls
            coEvery { mockQuery.listAliases(any()) } returns Either.Right(
                AliasListResult(
                    scopeId = "",
                    aliases = emptyList(),
                    totalCount = 0,
                ),
            )

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar {
                Ports(query = mockQuery, command = mockCommand) to Services(
                    errors = createErrorMapper(),
                    idempotency = createIdempotencyService(createArgumentCodec()),
                    codec = createArgumentCodec(),
                    logger = logger,
                )
            }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = PromptReference(name = "prompts.scopes.plan"),
                argument = CompleteRequest.Argument(name = "alias", value = "chi"),
            )

            val result = runBlocking { capturedHandler(req, null) }

            result.completion.values shouldHaveSize 2
            result.completion.values shouldContain "child-1"
            result.completion.values shouldContain "child-2"
        }

        "completion handler uses logger for debugging" {
            val server = mockk<Server>(relaxed = true)
            val mockQuery = mockk<ScopeManagementQueryPort>()
            val mockCommand = mockk<ScopeManagementCommandPort>()
            val mockLogger = mockk<Logger>(relaxed = true)

            every { mockLogger.isEnabledFor(any()) } returns true
            every { mockLogger.withContext(any()) } returns mockLogger
            every { mockLogger.withName(any()) } returns mockLogger

            coEvery { mockQuery.getRootScopes(any()) } returns Either.Right(
                ScopeListResult(scopes = emptyList(), totalCount = 0, offset = 0, limit = 200),
            )

            var capturedHandler: suspend (CompleteRequest, Any?) -> CompleteResult = { _, _ ->
                CompleteResult(CompleteResult.Completion(emptyList(), 0, false))
            }

            every {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    captureLambda(),
                )
            } answers {
                capturedHandler = lambda<suspend (CompleteRequest, Any?) -> CompleteResult>().captured
                Unit
            }

            val registrar = CompletionRegistrar { Ports(query = mockQuery, command = mockCommand) to TestFixtures.services(mockLogger) }

            registrar.register(server)

            // Test the captured handler
            val req = CompleteRequest(
                ref = PromptReference(name = "prompts.scopes.plan"),
                argument = CompleteRequest.Argument(name = "alias", value = "test"),
            )

            runBlocking { capturedHandler(req, null) }

            verify { mockLogger.debug(match { it.contains("Completions(alias)") }, any()) }
        }
    })
