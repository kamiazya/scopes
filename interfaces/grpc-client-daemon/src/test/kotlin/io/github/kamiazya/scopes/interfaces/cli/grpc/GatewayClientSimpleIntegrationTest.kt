package io.github.kamiazya.scopes.interfaces.cli.grpc

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView
import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

/**
 * Simplified integration tests for GatewayClient with in-process gRPC server.
 *
 * This test verifies essential GatewayClient methods work correctly through gRPC.
 * It uses correct method signatures and avoids complex scenarios.
 */
class GatewayClientSimpleIntegrationTest :
    DescribeSpec({

        lateinit var serverName: String
        lateinit var server: Server
        lateinit var channel: io.grpc.ManagedChannel
        lateinit var gatewayClient: TestGatewayClient
        val logger = ConsoleLogger()
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        beforeEach {
            // Create unique server name
            serverName = "gateway-test-${System.currentTimeMillis()}"

            // Create TaskGatewayService with test implementations
            val taskGatewayService = TaskGatewayServiceImpl(
                scopeManagementCommandPort = TestScopeManagementCommandPort(),
                scopeManagementQueryPort = TestScopeManagementQueryPort(),
                contextViewCommandPort = TestContextViewCommandPort(),
                contextViewQueryPort = TestContextViewQueryPort(),
                json = json,
                logger = logger,
            )

            // Build in-process server
            server = InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(taskGatewayService)
                .build()
                .start()

            // Create channel
            channel = InProcessChannelBuilder
                .forName(serverName)
                .directExecutor()
                .build()

            // Create test gateway client with the in-process channel
            gatewayClient = TestGatewayClient(
                logger = logger,
                json = json,
                testChannel = channel,
            )
        }

        afterEach {
            channel.shutdown()
            server.shutdown()
            server.awaitTermination()
        }

        describe("GatewayClient gRPC integration") {
            it("should properly serialize createScope command and deserialize result") {
                val result = gatewayClient.createScope(
                    title = "Test Scope",
                    description = "Test Description",
                    parentId = "parent-123",
                    customAlias = null,
                )

                result.shouldBeInstanceOf<Either.Right<*>>()
                val createResult = result.getOrNull()!!
                createResult.id shouldBe "test-123"
                createResult.title shouldBe "Test Scope"
                createResult.description shouldBe "Test Description"
                createResult.parentId shouldBe "parent-123"
            }

            it("should properly serialize updateScope command and deserialize result") {
                val result = gatewayClient.updateScope(
                    id = "scope-123",
                    title = "Updated Title",
                    description = "Updated Description",
                    parentId = null,
                )

                result.shouldBeInstanceOf<Either.Right<*>>()
                val updateResult = result.getOrNull()!!
                updateResult.id shouldBe "scope-123"
                updateResult.title shouldBe "Updated Title"
                updateResult.description shouldBe "Updated Description"
            }

            it("should properly serialize getScope query and deserialize result") {
                val result = gatewayClient.getScope(id = "scope-123")

                result.shouldBeInstanceOf<Either.Right<*>>()
                val scopeResult = result.getOrNull()!!
                scopeResult?.id shouldBe "scope-123"
                scopeResult?.title shouldBe "Test Scope"
            }

            it("should properly serialize getRootScopes query and deserialize results") {
                val result = gatewayClient.getRootScopes()

                result.shouldBeInstanceOf<Either.Right<*>>()
                val listResult = result.getOrNull()!!
                listResult.scopes.size shouldBe 2
                listResult.scopes[0].id shouldBe "root-1"
                listResult.scopes[1].id shouldBe "root-2"
            }

            it("should properly serialize addAlias command") {
                val result = gatewayClient.addAlias(
                    scopeId = "scope-123",
                    aliasName = "new-alias",
                )

                result.shouldBeInstanceOf<Either.Right<*>>()
                // Success means no exception thrown
            }

            it("should properly serialize removeAlias command") {
                val result = gatewayClient.removeAlias(
                    scopeId = "scope-123",
                    aliasName = "old-alias",
                )

                result.shouldBeInstanceOf<Either.Right<*>>()
                // Success means no exception thrown
            }

            it("should properly serialize setCanonicalAlias command") {
                val result = gatewayClient.setCanonicalAlias(
                    scopeId = "scope-123",
                    aliasName = "canonical-alias",
                )

                result.shouldBeInstanceOf<Either.Right<*>>()
                // Success means no exception thrown
            }
        }
    })

// ==== Test Helper Classes ====

// ==== Test Port Implementations ====

private class TestScopeManagementCommandPort : ScopeManagementCommandPort {
    override suspend fun createScope(command: CreateScopeCommand) = Either.Right(
        CreateScopeResult(
            id = "test-123",
            title = command.title,
            description = command.description,
            parentId = (command as? CreateScopeCommand.WithAutoAlias)?.parentId,
            canonicalAlias = "test-alias",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ),
    )

    override suspend fun updateScope(command: UpdateScopeCommand) = Either.Right(
        UpdateScopeResult(
            id = command.id,
            title = command.title ?: "Original Title",
            description = command.description,
            parentId = command.parentId,
            canonicalAlias = "test-alias",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ),
    )

    override suspend fun deleteScope(command: DeleteScopeCommand) = Either.Right(Unit)
    override suspend fun addAlias(command: AddAliasCommand) = Either.Right(Unit)
    override suspend fun removeAlias(command: RemoveAliasCommand) = Either.Right(Unit)
    override suspend fun setCanonicalAlias(command: SetCanonicalAliasCommand) = Either.Right(Unit)
    override suspend fun renameAlias(command: RenameAliasCommand) = Either.Right(Unit)
}

private class TestScopeManagementQueryPort : ScopeManagementQueryPort {
    override suspend fun getScope(query: GetScopeQuery) = Either.Right(
        ScopeResult(
            id = query.id,
            title = "Test Scope",
            description = "Test Description",
            parentId = null,
            canonicalAlias = "test-alias",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ),
    )

    override suspend fun getRootScopes(query: GetRootScopesQuery) = Either.Right(
        ScopeListResult(
            scopes = listOf(
                ScopeResult(
                    id = "root-1",
                    title = "Root Scope 1",
                    description = null,
                    parentId = null,
                    canonicalAlias = "root-alias-1",
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                ),
                ScopeResult(
                    id = "root-2",
                    title = "Root Scope 2",
                    description = null,
                    parentId = null,
                    canonicalAlias = "root-alias-2",
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                ),
            ),
            totalCount = 2,
            offset = 0,
            limit = 50,
        ),
    )

    override suspend fun getChildren(query: GetChildrenQuery) = Either.Right(
        ScopeListResult(
            scopes = emptyList(),
            totalCount = 0,
            offset = 0,
            limit = 50,
        ),
    )

    override suspend fun getScopeByAlias(query: GetScopeByAliasQuery) = Either.Right(
        ScopeResult(
            id = "test-scope-by-alias",
            title = "Scope Found By Alias",
            description = null,
            parentId = null,
            canonicalAlias = query.aliasName,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ),
    )

    override suspend fun listAliases(query: ListAliasesQuery) = Either.Right(
        AliasListResult(
            scopeId = query.scopeId,
            aliases = emptyList(),
            totalCount = 0,
        ),
    )

    override suspend fun listScopesWithAspect(query: ListScopesWithAspectQuery): Either<ScopeContractError, List<ScopeResult>> = Either.Right(emptyList())

    override suspend fun listScopesWithQuery(query: ListScopesWithQueryQuery): Either<ScopeContractError, List<ScopeResult>> = Either.Right(emptyList())
}

private class TestContextViewCommandPort : ContextViewCommandPort {
    override suspend fun createContextView(command: CreateContextViewCommand) = Either.Right(Unit)
    override suspend fun updateContextView(command: UpdateContextViewCommand) = Either.Right(Unit)
    override suspend fun deleteContextView(command: DeleteContextViewCommand) = Either.Right(Unit)
    override suspend fun setActiveContext(command: SetActiveContextCommand) = Either.Right(Unit)
    override suspend fun clearActiveContext() = Either.Right(Unit)
}

private class TestContextViewQueryPort : ContextViewQueryPort {
    override suspend fun getContextView(query: GetContextViewQuery): Either<ScopeContractError, ContextView?> = Either.Right(
        ContextView(
            key = query.key,
            name = "Work Context",
            description = "My work items",
            filter = "type=work",
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ),
    )

    override suspend fun listContextViews(query: ListContextViewsQuery): Either<ScopeContractError, List<ContextView>> = Either.Right(
        listOf(
            ContextView(
                key = "work-context",
                name = "Work Context",
                description = "Work items",
                filter = "type=work",
                createdAt = kotlinx.datetime.Clock.System.now(),
                updatedAt = kotlinx.datetime.Clock.System.now(),
            ),
        ),
    )

    override suspend fun getActiveContext(query: GetActiveContextQuery): Either<ScopeContractError, ContextView?> = Either.Right(null)
}
