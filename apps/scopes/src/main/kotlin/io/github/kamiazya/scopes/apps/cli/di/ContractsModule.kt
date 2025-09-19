package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.userpreferences.UserPreferencesQueryPort
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.AspectCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.AspectQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ContextViewCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ContextViewQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ScopeManagementCommandPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ScopeManagementQueryPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.UserPreferencesToHierarchyPolicyAdapter
import io.github.kamiazya.scopes.userpreferences.infrastructure.adapters.UserPreferencesQueryPortAdapter
import org.koin.dsl.module

/**
 * Koin module for Contracts layer components
 *
 * This module provides:
 * - Contract port implementations that bridge between bounded contexts
 * - Error mappers for translating domain errors to contract errors
 * - Anti-Corruption Layer adapters for cross-context integration
 */
val contractsModule = module {
    // Error Mappers - Removed as they are now created inside adapters

    // Contract Port Implementations
    single<ScopeManagementCommandPort> {
        ScopeManagementCommandPortAdapter(
            createScopeHandler = get(),
            updateScopeHandler = get(),
            deleteScopeHandler = get(),
            getScopeByIdHandler = get(),
            addAliasHandler = get(),
            removeAliasHandler = get(),
            setCanonicalAliasHandler = get(),
            renameAliasHandler = get(),
            transactionManager = get(),
            applicationErrorMapper = get(),
        )
    }
    single<ScopeManagementQueryPort> {
        ScopeManagementQueryPortAdapter(
            getScopeByIdHandler = get(),
            getScopeByAliasHandler = get(),
            getChildrenHandler = get(),
            getRootScopesHandler = get(),
            listAliasesHandler = get(),
            filterScopesWithQueryHandler = get(),
            applicationErrorMapper = get(),
        )
    }

    single<UserPreferencesQueryPort> {
        UserPreferencesQueryPortAdapter(
            getCurrentUserPreferencesHandler = get(),
            errorMapper = get(),
            logger = get(),
        )
    }

    single<ContextViewCommandPort> {
        ContextViewCommandPortAdapter(
            createContextViewHandler = get(),
            updateContextViewHandler = get(),
            deleteContextViewHandler = get(),
            activeContextService = get(),
            applicationErrorMapper = get(),
        )
    }

    single<ContextViewQueryPort> {
        ContextViewQueryPortAdapter(
            listContextViewsHandler = get(),
            getContextViewHandler = get(),
            activeContextService = get(),
            logger = get(),
        )
    }

    single<AspectCommandPort> {
        AspectCommandPortAdapter(
            defineAspectHandler = get(),
            updateAspectDefinitionHandler = get(),
            deleteAspectDefinitionHandler = get(),
        )
    }

    single<AspectQueryPort> {
        AspectQueryPortAdapter(
            getAspectDefinitionHandler = get(),
            listAspectDefinitionsHandler = get(),
            validateAspectValueUseCase = get(),
            timeProvider = get(),
            logger = get(),
        )
    }

    // Anti-Corruption Layer Adapters
    single<HierarchyPolicyProvider> {
        UserPreferencesToHierarchyPolicyAdapter(
            userPreferencesPort = get(),
            logger = get(),
        )
    }
}
