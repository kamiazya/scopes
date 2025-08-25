package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import org.koin.dsl.module

/**
 * Koin module for Scope Management bounded context
 *
 * This module defines all dependencies within the Scope Management context including:
 * - Domain services
 * - Application services
 * - Use case handlers
 */
val scopeManagementModule = module {
    // Domain Services
    single { ScopeHierarchyService() }
    single {
        io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService(
            aliasRepository = get(),
            aliasGenerationService = get(),
        )
    }

    // Domain Factory
    single {
        ScopeFactory(
            hierarchyService = get(),
            scopeRepository = get(),
        )
    }

    // Application Services
    single { CrossAggregateValidationService(scopeRepository = get()) }
    single {
        io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService(
            contextViewRepository = get(),
        )
    }

    // Use Case Handlers
    single {
        CreateScopeHandler(
            scopeFactory = get(),
            scopeRepository = get(),
            scopeAliasRepository = get(),
            aliasGenerationService = get(),
            transactionManager = get(),
            hierarchyPolicyProvider = get(),
            logger = get(),
        )
    }

    single {
        UpdateScopeHandler(
            scopeRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        DeleteScopeHandler(
            scopeRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        GetScopeByIdHandler(
            scopeRepository = get(),
            logger = get(),
        )
    }

    single {
        GetChildrenHandler(
            scopeRepository = get(),
            logger = get(),
        )
    }

    single {
        GetRootScopesHandler(
            scopeRepository = get(),
            logger = get(),
        )
    }
}
