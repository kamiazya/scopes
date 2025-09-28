package io.github.kamiazya.scopes.apps.daemon.di

import arrow.core.Either
import io.github.kamiazya.scopes.apps.daemon.DaemonApplication
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.interfaces.daemon.GrpcServerFactory
import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.infrastructure.transaction.NoOpTransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationType
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.policy.DefaultHierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.util.Properties

/**
 * Koin module for the Scopes daemon application.
 *
 * This module configures all the dependencies needed for the daemon,
 * including gRPC services, port adapters, and infrastructure.
 */
val daemonSpecificModule = module {
    // Application info
    single {
        // Load build info from resources
        val buildInfo = Properties()
        try {
            ApplicationInfo::class.java.getResourceAsStream("/build-info.properties")?.use { stream ->
                buildInfo.load(stream)
            }
        } catch (e: Exception) {
            // Ignore errors loading build info
        }

        ApplicationInfo(
            name = "scopesd",
            version = buildInfo.getProperty("version", "0.1.0-SNAPSHOT"),
            type = ApplicationType.DAEMON,
            gitRevision = buildInfo.getProperty("git.revision"),
            startTime = Clock.System.now(),
        )
    }

    // Logger
    single<Logger> { ConsoleLogger() }

    // Scope Management Infrastructure - Minimal implementation for M1
    single<ScopeRepository> { InMemoryScopeRepository() }
    single<ScopeAliasRepository> { InMemoryScopeAliasRepository() }
    single<TransactionManager> { NoOpTransactionManager() }

    // Alias generation components
    single<WordProvider> { DefaultWordProvider() }
    single<AliasGenerationStrategy> { HaikunatorStrategy() }
    single<AliasGenerationService> { DefaultAliasGenerationService(get(), get()) }

    // Hierarchy services
    single<HierarchyPolicyProvider> { DefaultHierarchyPolicyProvider() }
    single<ScopeHierarchyService> { ScopeHierarchyService() }
    single<ScopeHierarchyApplicationService> { ScopeHierarchyApplicationService(get(), get()) }

    // Factory and mapper
    single<ApplicationErrorMapper> { ApplicationErrorMapper(get()) }
    single<ScopeFactory> { ScopeFactory(get(), get(), get()) }

    // Command Handler for CreateScope
    single<CreateScopeHandler> {
        CreateScopeHandler(
            scopeFactory = get(),
            scopeRepository = get(),
            scopeAliasRepository = get(),
            aliasGenerationService = get(),
            transactionManager = get(),
            hierarchyPolicyProvider = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    // Query Handlers for M2
    single<GetScopeByIdHandler> {
        GetScopeByIdHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single<GetRootScopesHandler> {
        GetRootScopesHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single<GetChildrenHandler> {
        GetChildrenHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    // Port implementations - Minimal implementation for CreateScope only
    single<ScopeManagementCommandPort> {
        // Create a minimal command port that only supports CreateScope
        MinimalCreateScopeCommandPortAdapter(
            createScopeHandler = get(),
            logger = get(),
        )
    }

    // Query port implementation for M2
    single<ScopeManagementQueryPort> {
        MinimalQueryPortAdapter(
            getScopeByIdHandler = get(),
            getRootScopesHandler = get(),
            logger = get(),
            getChildrenHandler = get(),
        )
    }

    // JSON serializer
    single<Json> { Json { ignoreUnknownKeys = true } }

    // TaskGateway service for M1/M2 implementation
    single<TaskGatewayServiceImpl> {
        TaskGatewayServiceImpl(
            scopeManagementCommandPort = get(),
            scopeManagementQueryPort = get(), // M2: Query support added
            contextViewCommandPort = getOrNull(), // M3: Context view support (optional)
            contextViewQueryPort = getOrNull(), // M3: Context view query (optional)
            json = get(),
            logger = get(),
        )
    }

    // gRPC Server Factory - for Phase 2/M1, with TaskGateway service
    single {
        GrpcServerFactory(taskGatewayService = get())
    }

    // Daemon application
    single {
        DaemonApplication(
            logger = get(),
            applicationInfo = get(),
            serverFactory = get(),
        )
    }
}

/**
 * Combined module that includes all necessary dependencies for the daemon.
 * For Phase 2.5/M1, this includes minimal scope management for CreateScope Happy Path.
 */
val daemonModule = daemonSpecificModule

/**
 * Minimal query port adapter that supports limited query operations for M2.
 * Only implements getScope and getRootScopes using the available handlers.
 *
 * All other operations return UNIMPLEMENTED errors.
 */
class MinimalQueryPortAdapter(
    private val getScopeByIdHandler: GetScopeByIdHandler,
    private val getRootScopesHandler: GetRootScopesHandler,
    private val logger: Logger,
    private val getChildrenHandler: GetChildrenHandler = GetChildrenHandler(
        scopeRepository = InMemoryScopeRepository(),
        aliasRepository = InMemoryScopeAliasRepository(),
        transactionManager = NoOpTransactionManager(),
        applicationErrorMapper = ApplicationErrorMapper(logger),
        logger = logger,
    ),
) : ScopeManagementQueryPort {

    override suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?> {
        logger.debug("MinimalQueryPortAdapter: handling getScope", mapOf("scopeId" to query.id))
        // Map contract query to application query
        val applicationQuery = io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById(query.id)
        return getScopeByIdHandler(applicationQuery)
    }

    override suspend fun getRootScopes(
        query: GetRootScopesQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> {
        logger.debug("MinimalQueryPortAdapter: handling getRootScopes", mapOf("offset" to query.offset, "limit" to query.limit))
        // Map contract query to application query
        val applicationQuery = io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetRootScopes(
            offset = query.offset,
            limit = query.limit,
        )
        return getRootScopesHandler(applicationQuery)
    }

    override suspend fun getChildren(
        query: GetChildrenQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> {
        logger.debug("MinimalQueryPortAdapter: handling getChildren", mapOf("parentId" to query.parentId, "offset" to query.offset, "limit" to query.limit))
        val applicationQuery = io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetChildren(
            parentId = query.parentId,
            offset = query.offset,
            limit = query.limit,
        )
        return getChildrenHandler(applicationQuery)
    }

    override suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult> {
        logger.warn("MinimalQueryPortAdapter: getScopeByAlias not implemented in M2", mapOf("aliasName" to query.aliasName))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias-lookup",
            ),
        )
    }

    override suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult> {
        logger.warn("MinimalQueryPortAdapter: listAliases not implemented in M2", mapOf("scopeId" to query.scopeId))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias-list",
            ),
        )
    }

    override suspend fun listScopesWithAspect(query: ListScopesWithAspectQuery): Either<ScopeContractError, List<ScopeResult>> {
        logger.warn("MinimalQueryPortAdapter: listScopesWithAspect not implemented in M2", mapOf("aspectKey" to query.aspectKey))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-aspect-query",
            ),
        )
    }

    override suspend fun listScopesWithQuery(query: ListScopesWithQueryQuery): Either<ScopeContractError, List<ScopeResult>> {
        logger.warn("MinimalQueryPortAdapter: listScopesWithQuery not implemented in M2", mapOf("aspectQuery" to query.aspectQuery))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-advanced-query",
            ),
        )
    }
}

/**
 * Minimal command port adapter that only supports CreateScope operation.
 * Used for M1 practical implementation to enable CreateScope Happy Path.
 *
 * All other operations return UNIMPLEMENTED errors.
 */
class MinimalCreateScopeCommandPortAdapter(private val createScopeHandler: CreateScopeHandler, private val logger: Logger) : ScopeManagementCommandPort {

    override suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> {
        logger.debug("MinimalCreateScopeCommandPortAdapter: handling createScope", mapOf("title" to command.title))
        return createScopeHandler(command)
    }

    override suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: updateScope not implemented in M1", mapOf("scopeId" to command.id))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-update",
            ),
        )
    }

    override suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: deleteScope not implemented in M1", mapOf("scopeId" to command.id))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-delete",
            ),
        )
    }

    override suspend fun addAlias(command: AddAliasCommand): Either<ScopeContractError, Unit> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: addAlias not implemented in M1", mapOf("scopeId" to command.scopeId))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias",
            ),
        )
    }

    override suspend fun removeAlias(command: RemoveAliasCommand): Either<ScopeContractError, Unit> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: removeAlias not implemented in M1", mapOf("alias" to command.aliasName))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias",
            ),
        )
    }

    override suspend fun setCanonicalAlias(command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: setCanonicalAlias not implemented in M1", mapOf("scopeId" to command.scopeId))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias",
            ),
        )
    }

    override suspend fun renameAlias(command: RenameAliasCommand): Either<ScopeContractError, Unit> {
        logger.warn("MinimalCreateScopeCommandPortAdapter: renameAlias not implemented in M1", mapOf("oldAlias" to command.oldAliasName))
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management-alias",
            ),
        )
    }
}
