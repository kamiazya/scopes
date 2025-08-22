package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction.NoopTransactionManager

/**
 * Composition Root for dependency injection.
 * This is where all the dependencies are wired together.
 *
 * Following the Composition Root pattern:
 * - All dependencies are created here
 * - No service locator or global state
 * - Dependencies are injected through constructors
 * - This is the only place where concrete implementations are referenced
 */
object CompositionRoot {

    // Infrastructure layer
    private val logger: Logger = ConsoleLogger()
    private val scopeRepository: ScopeRepository = InMemoryScopeRepository()
    private val transactionManager: TransactionManager = NoopTransactionManager()

    // Domain services
    private val scopeHierarchyService = ScopeHierarchyService()

    // Application services
    private val crossAggregateValidationService = CrossAggregateValidationService(scopeRepository)

    // Use case handlers
    val createScopeHandler = CreateScopeHandler(
        scopeRepository = scopeRepository,
        transactionManager = transactionManager,
        hierarchyService = scopeHierarchyService,
        crossAggregateValidationService = crossAggregateValidationService,
        logger = logger,
    )

    val getScopeByIdHandler = GetScopeByIdHandler(
        scopeRepository = scopeRepository,
        logger = logger,
    )

    val updateScopeHandler = UpdateScopeHandler(
        scopeRepository = scopeRepository,
        transactionManager = transactionManager,
        logger = logger,
    )

    val deleteScopeHandler = DeleteScopeHandler(
        scopeRepository = scopeRepository,
        transactionManager = transactionManager,
        logger = logger,
    )

    val getChildrenHandler = GetChildrenHandler(
        scopeRepository = scopeRepository,
        logger = logger,
    )

    val getRootScopesHandler = GetRootScopesHandler(
        scopeRepository = scopeRepository,
        logger = logger,
    )
}
