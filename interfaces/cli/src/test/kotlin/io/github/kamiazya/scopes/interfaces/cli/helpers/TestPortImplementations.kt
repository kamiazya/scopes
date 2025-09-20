package io.github.kamiazya.scopes.interfaces.cli.helpers

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import io.github.kamiazya.scopes.platform.infrastructure.transaction.NoOpTransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DefineAspectHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DeleteAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.UpdateAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.DeleteContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.UpdateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.GetAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.ListAspectDefinitionsHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.*
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.*
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.service.ContextAuditService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.AspectCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.AspectQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ContextViewCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ContextViewQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ScopeManagementCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ScopeManagementQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.event.NoOpDomainEventPublisher
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import kotlinx.datetime.Clock

/**
 * Factory for creating real port implementations for integration tests.
 * Uses in-memory repositories and actual business logic implementations.
 */
object TestPortImplementations {

    /**
     * Creates a complete test environment with all necessary dependencies.
     */
    fun createTestEnvironment(): TestEnvironment {
        // Create repositories
        val scopeRepository = InMemoryScopeRepository()
        val scopeAliasRepository = InMemoryScopeAliasRepository()
        val contextViewRepository = InMemoryContextViewRepository()
        val aspectDefinitionRepository = InMemoryAspectDefinitionRepository()
        val activeContextRepository = InMemoryActiveContextRepository()

        // Create utility components
        val logger = ConsoleLogger(name = "Test")
        val transactionManager = NoOpTransactionManager()
        val applicationErrorMapper = ApplicationErrorMapper(logger)

        // Create domain services
        val wordProvider = DefaultWordProvider()
        val strategy = HaikunatorStrategy()
        val aliasGenerationService = DefaultAliasGenerationService(strategy, wordProvider)
        val hierarchyPolicyProvider = DefaultHierarchyPolicyProvider()
        val hierarchyService = ScopeHierarchyService()

        // Create event publisher and audit service first
        val eventPublisher: DomainEventPublisher = NoOpDomainEventPublisher()
        val contextAuditService = ContextAuditService(eventPublisher)

        // Create application services
        val scopeHierarchyApplicationService = ScopeHierarchyApplicationService(
            repository = scopeRepository,
            domainService = hierarchyService,
        )

        val aspectUsageValidationService = AspectUsageValidationService(
            scopeRepository = scopeRepository,
        )

        val aspectValueValidationService = io.github.kamiazya.scopes.scopemanagement.domain.service.validation.AspectValueValidationService()

        val validateAspectValueUseCase = ValidateAspectValueUseCase(
            aspectDefinitionRepository = aspectDefinitionRepository,
            validationService = aspectValueValidationService,
        )

        val timeProvider = object : TimeProvider {
            override fun now() = Clock.System.now()
        }

        val scopeFactory = ScopeFactory(
            scopeRepository = scopeRepository,
            hierarchyApplicationService = scopeHierarchyApplicationService,
            hierarchyService = hierarchyService,
        )

        val scopeAliasApplicationService = ScopeAliasApplicationService(
            aliasRepository = scopeAliasRepository,
            aliasGenerationService = aliasGenerationService,
        )

        val activeContextService = ActiveContextService(
            contextViewRepository = contextViewRepository,
            activeContextRepository = activeContextRepository,
            contextAuditService = contextAuditService,
        )

        // Create command handlers
        val createScopeHandler = CreateScopeHandler(
            scopeFactory = scopeFactory,
            scopeRepository = scopeRepository,
            scopeAliasRepository = scopeAliasRepository,
            aliasGenerationService = aliasGenerationService,
            transactionManager = transactionManager,
            hierarchyPolicyProvider = hierarchyPolicyProvider,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val updateScopeHandler = UpdateScopeHandler(
            scopeRepository = scopeRepository,
            scopeAliasRepository = scopeAliasRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val deleteScopeHandler = DeleteScopeHandler(
            scopeRepository = scopeRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val addAliasHandler = AddAliasHandler(
            scopeAliasService = scopeAliasApplicationService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val removeAliasHandler = RemoveAliasHandler(
            scopeAliasService = scopeAliasApplicationService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val setCanonicalAliasHandler = SetCanonicalAliasHandler(
            scopeAliasService = scopeAliasApplicationService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val renameAliasHandler = RenameAliasHandler(
            scopeAliasService = scopeAliasApplicationService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        // Create query handlers
        val getScopeByIdHandler = GetScopeByIdHandler(
            scopeRepository = scopeRepository,
            aliasRepository = scopeAliasRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val getScopeByAliasHandler = GetScopeByAliasHandler(
            scopeAliasRepository = scopeAliasRepository,
            scopeRepository = scopeRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val getChildrenHandler = GetChildrenHandler(
            scopeRepository = scopeRepository,
            aliasRepository = scopeAliasRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val getRootScopesHandler = GetRootScopesHandler(
            scopeRepository = scopeRepository,
            aliasRepository = scopeAliasRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        val listAliasesHandler = ListAliasesHandler(
            scopeAliasRepository = scopeAliasRepository,
            scopeRepository = scopeRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        // ResolveAliasHandler doesn't exist, remove it

        // GetScopeHierarchyPathHandler doesn't exist, remove it

        val filterScopesHandler = FilterScopesWithQueryHandler(
            scopeRepository = scopeRepository,
            aliasRepository = scopeAliasRepository,
            aspectDefinitionRepository = aspectDefinitionRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
            logger = logger,
        )

        // Create context handlers
        val createContextViewHandler = CreateContextViewHandler(
            contextViewRepository = contextViewRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val updateContextViewHandler = UpdateContextViewHandler(
            contextViewRepository = contextViewRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val deleteContextViewHandler = DeleteContextViewHandler(
            contextViewRepository = contextViewRepository,
            activeContextService = activeContextService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val getContextViewHandler = GetContextViewHandler(
            contextViewRepository = contextViewRepository,
            transactionManager = transactionManager,
            logger = logger,
        )

        val listContextViewsHandler = ListContextViewsHandler(
            contextViewRepository = contextViewRepository,
            transactionManager = transactionManager,
            logger = logger,
        )

        val getFilteredScopesHandler = GetFilteredScopesHandler(
            scopeRepository = scopeRepository,
            contextViewRepository = contextViewRepository,
            activeContextRepository = activeContextRepository,
            aspectDefinitionRepository = aspectDefinitionRepository,
            contextAuditService = contextAuditService,
            transactionManager = transactionManager,
            logger = logger,
        )

        // Create aspect handlers
        val defineAspectHandler = DefineAspectHandler(
            aspectDefinitionRepository = aspectDefinitionRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val updateAspectDefinitionHandler = UpdateAspectDefinitionHandler(
            aspectDefinitionRepository = aspectDefinitionRepository,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val deleteAspectDefinitionHandler = DeleteAspectDefinitionHandler(
            aspectDefinitionRepository = aspectDefinitionRepository,
            aspectUsageValidationService = aspectUsageValidationService,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val getAspectDefinitionHandler = GetAspectDefinitionHandler(
            aspectDefinitionRepository = aspectDefinitionRepository,
            transactionManager = transactionManager,
            logger = logger,
        )

        val listAspectDefinitionsHandler = ListAspectDefinitionsHandler(
            aspectDefinitionRepository = aspectDefinitionRepository,
            transactionManager = transactionManager,
            logger = logger,
        )

        // Create port adapters
        val scopeManagementCommandPort = ScopeManagementCommandPortAdapter(
            createScopeHandler = createScopeHandler,
            updateScopeHandler = updateScopeHandler,
            deleteScopeHandler = deleteScopeHandler,
            getScopeByIdHandler = getScopeByIdHandler,
            addAliasHandler = addAliasHandler,
            removeAliasHandler = removeAliasHandler,
            setCanonicalAliasHandler = setCanonicalAliasHandler,
            renameAliasHandler = renameAliasHandler,
            transactionManager = transactionManager,
            applicationErrorMapper = applicationErrorMapper,
        )

        val scopeManagementQueryPort = ScopeManagementQueryPortAdapter(
            getScopeByIdHandler = getScopeByIdHandler,
            getScopeByAliasHandler = getScopeByAliasHandler,
            getChildrenHandler = getChildrenHandler,
            getRootScopesHandler = getRootScopesHandler,
            listAliasesHandler = listAliasesHandler,
            filterScopesWithQueryHandler = filterScopesHandler,
            applicationErrorMapper = applicationErrorMapper,
        )

        val contextViewCommandPort = ContextViewCommandPortAdapter(
            createContextViewHandler = createContextViewHandler,
            updateContextViewHandler = updateContextViewHandler,
            deleteContextViewHandler = deleteContextViewHandler,
            activeContextService = activeContextService,
            applicationErrorMapper = applicationErrorMapper,
        )

        val contextViewQueryPort = ContextViewQueryPortAdapter(
            listContextViewsHandler = listContextViewsHandler,
            getContextViewHandler = getContextViewHandler,
            activeContextService = activeContextService,
            logger = logger,
        )

        val aspectCommandPort = AspectCommandPortAdapter(
            defineAspectHandler = defineAspectHandler,
            updateAspectDefinitionHandler = updateAspectDefinitionHandler,
            deleteAspectDefinitionHandler = deleteAspectDefinitionHandler,
        )

        val aspectQueryPort = AspectQueryPortAdapter(
            getAspectDefinitionHandler = getAspectDefinitionHandler,
            listAspectDefinitionsHandler = listAspectDefinitionsHandler,
            validateAspectValueUseCase = validateAspectValueUseCase,
            timeProvider = timeProvider,
            logger = logger,
        )

        return TestEnvironment(
            scopeManagementCommandPort = scopeManagementCommandPort,
            scopeManagementQueryPort = scopeManagementQueryPort,
            contextViewCommandPort = contextViewCommandPort,
            contextViewQueryPort = contextViewQueryPort,
            aspectCommandPort = aspectCommandPort,
            aspectQueryPort = aspectQueryPort,
            repositories = TestRepositories(
                scopeRepository = scopeRepository,
                scopeAliasRepository = scopeAliasRepository,
                contextViewRepository = contextViewRepository,
                aspectDefinitionRepository = aspectDefinitionRepository,
            ),
        )
    }

    /**
     * Container for all test dependencies
     */
    data class TestEnvironment(
        val scopeManagementCommandPort: ScopeManagementCommandPort,
        val scopeManagementQueryPort: ScopeManagementQueryPort,
        val contextViewCommandPort: ContextViewCommandPort,
        val contextViewQueryPort: ContextViewQueryPort,
        val aspectCommandPort: AspectCommandPort,
        val aspectQueryPort: AspectQueryPort,
        val repositories: TestRepositories,
    )

    /**
     * Container for repository instances for direct access in tests
     */
    data class TestRepositories(
        val scopeRepository: ScopeRepository,
        val scopeAliasRepository: ScopeAliasRepository,
        val contextViewRepository: ContextViewRepository,
        val aspectDefinitionRepository: AspectDefinitionRepository,
    )

    /**
     * Default hierarchy policy provider for testing
     */
    private class DefaultHierarchyPolicyProvider : HierarchyPolicyProvider {
        override suspend fun getPolicy() = Either.Right(HierarchyPolicy.default())
    }
}
