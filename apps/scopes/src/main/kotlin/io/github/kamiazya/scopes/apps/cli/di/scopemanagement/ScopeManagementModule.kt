package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.application.command.DefineAspectUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.aspect.DeleteAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.aspect.UpdateAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.CreateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.DeleteContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.UpdateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.error.EventStoreErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.handler.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.FilterScopesWithQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.application.query.FilterScopesWithQueryUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.aspect.GetAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.aspect.ListAspectDefinitionsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.context.GetContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.context.ListContextViewsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.service.ContextAuditService
import io.github.kamiazya.scopes.scopemanagement.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.ScopeHierarchyValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.ScopeUniquenessValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasPolicy
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
    single { AspectValueValidationService() }
    single { ScopeHierarchyValidationService(scopeRepository = get()) }
    single { ScopeUniquenessValidationService(scopeRepository = get()) }
    single { AspectUsageValidationService(scopeRepository = get()) }

    // Query Components
    single { AspectQueryParser() }

    // Application Services
    single { ScopeAliasPolicy() }
    single {
        ScopeAliasApplicationService(
            aliasRepository = get(),
            aliasGenerationService = get(),
            aliasPolicy = get(),
        )
    }
    single {
        ScopeHierarchyApplicationService(
            repository = get(),
            domainService = get(),
        )
    }
    single {
        CrossAggregateValidationService(
            hierarchyValidationService = get(),
            uniquenessValidationService = get(),
        )
    }

    // Event publishing (temporary no-op implementation)
    single<DomainEventPublisher> {
        object : DomainEventPublisher {
            override suspend fun publish(event: DomainEvent) {
                // TODO: Implement proper event publishing
            }
        }
    }

    single {
        ContextAuditService(
            eventPublisher = get(),
        )
    }
    single {
        ActiveContextService(
            contextViewRepository = get(),
            activeContextRepository = get(),
            contextAuditService = get(),
        )
    }

    // Factories
    single {
        ScopeFactory(
            scopeRepository = get(),
            hierarchyApplicationService = get(),
            hierarchyService = get(),
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

    single {
        GetScopeByAliasHandler(
            scopeAliasService = get(),
            scopeRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    // Alias Handlers
    single {
        AddAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        RemoveAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        ListAliasesHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        SetCanonicalAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        RenameAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    // Aspect Definition Use Cases
    single {
        DefineAspectUseCase(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        GetAspectDefinitionUseCase(
            aspectDefinitionRepository = get(),
        )
    }

    single {
        UpdateAspectDefinitionUseCase(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        DeleteAspectDefinitionUseCase(
            aspectDefinitionRepository = get(),
            aspectUsageValidationService = get(),
            transactionManager = get(),
        )
    }

    single {
        ListAspectDefinitionsUseCase(
            aspectDefinitionRepository = get(),
        )
    }

    single {
        ValidateAspectValueUseCase(
            aspectDefinitionRepository = get(),
            validationService = get(),
        )
    }

    // Query Use Case
    single {
        FilterScopesWithQueryUseCase(
            scopeRepository = get(),
            aspectDefinitionRepository = get(),
        )
    }

    // Query Handler
    single {
        FilterScopesWithQueryHandler(
            filterScopesWithQueryUseCase = get(),
            logger = get(),
        )
    }

    // Context View Use Cases
    single {
        CreateContextViewUseCase(
            contextViewRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        ListContextViewsUseCase(
            contextViewRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        GetContextViewUseCase(
            contextViewRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        UpdateContextViewUseCase(
            contextViewRepository = get(),
            transactionManager = get(),
        )
    }

    single {
        DeleteContextViewUseCase(
            contextViewRepository = get(),
            transactionManager = get(),
            activeContextService = get(),
        )
    }

    // Error Mappers
    single {
        EventStoreErrorMapper(logger = get())
    }
}
