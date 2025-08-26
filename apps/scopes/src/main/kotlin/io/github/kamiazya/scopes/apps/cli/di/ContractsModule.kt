package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementPort
import io.github.kamiazya.scopes.contracts.userpreferences.UserPreferencesPort
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ScopeManagementPortAdapter
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.UserPreferencesToHierarchyPolicyAdapter
import io.github.kamiazya.scopes.userpreferences.infrastructure.adapters.UserPreferencesPortAdapter
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
    single<ScopeManagementPort> {
        ScopeManagementPortAdapter(
            createScopeHandler = get(),
            updateScopeHandler = get(),
            deleteScopeHandler = get(),
            getScopeByIdHandler = get(),
            getScopeByAliasHandler = get(),
            getChildrenHandler = get(),
            getRootScopesHandler = get(),
            transactionManager = get(),
            logger = get(),
        )
    }

    single<UserPreferencesPort> {
        UserPreferencesPortAdapter(
            getCurrentUserPreferencesHandler = get(),
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
