package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
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
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.GetAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.ListAspectDefinitionsHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetFilteredScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.ListContextViewsHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.FilterScopesWithQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.service.ContextAuditService
import io.github.kamiazya.scopes.scopemanagement.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.ScopeHierarchyValidationService
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.ScopeUniquenessValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.ScopeAliasPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.service.filter.FilterEvaluationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.ContextViewValidationService
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
    single { ContextViewValidationService() }
    single { FilterEvaluationService() }
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
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        UpdateScopeHandler(
            scopeRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        DeleteScopeHandler(
            scopeRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        GetScopeByIdHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        GetChildrenHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        GetRootScopesHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        GetScopeByAliasHandler(
            scopeAliasRepository = get(),
            scopeRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    // Alias Handlers
    single {
        AddAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        RemoveAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        ListAliasesHandler(
            scopeAliasRepository = get(),
            scopeRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        SetCanonicalAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    single {
        RenameAliasHandler(
            scopeAliasService = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    // Aspect Definition Handlers
    single {
        DefineAspectHandler(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }

    single {
        GetAspectDefinitionHandler(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        UpdateAspectDefinitionHandler(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }

    single {
        DeleteAspectDefinitionHandler(
            aspectDefinitionRepository = get(),
            aspectUsageValidationService = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }

    single {
        ListAspectDefinitionsHandler(
            aspectDefinitionRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        ValidateAspectValueUseCase(
            aspectDefinitionRepository = get(),
            validationService = get(),
        )
    }

    // Query Handler
    single {
        FilterScopesWithQueryHandler(
            scopeRepository = get(),
            aliasRepository = get(),
            aspectDefinitionRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
            logger = get(),
        )
    }

    // Context View Handlers
    single {
        CreateContextViewHandler(
            contextViewRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }

    single {
        ListContextViewsHandler(
            contextViewRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        GetContextViewHandler(
            contextViewRepository = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        GetFilteredScopesHandler(
            scopeRepository = get(),
            contextViewRepository = get(),
            activeContextRepository = get(),
            aspectDefinitionRepository = get(),
            contextAuditService = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single {
        UpdateContextViewHandler(
            contextViewRepository = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }

    single {
        DeleteContextViewHandler(
            contextViewRepository = get(),
            transactionManager = get(),
            activeContextService = get(),
            applicationErrorMapper = get(),
        )
    }

    // Error Mappers
    single { ApplicationErrorMapper(get()) }
}
