package io.github.kamiazya.scopes.interfaces.shared.di

import io.github.kamiazya.scopes.interfaces.shared.adapters.UserPreferencesToHierarchyPolicyAdapter
import io.github.kamiazya.scopes.interfaces.shared.facade.ScopeManagementFacade
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import org.koin.dsl.module

/**
 * Koin module for shared interface components
 *
 * This module provides:
 * - Shared facades that aggregate multiple bounded contexts
 * - Adapters for cross-context integration (Anti-Corruption Layer)
 * - Common interface utilities
 */
val interfaceSharedModule = module {
    // Adapters (Anti-Corruption Layer)
    single<HierarchyPolicyProvider> {
        UserPreferencesToHierarchyPolicyAdapter(
            userPreferencesService = get(),
            logger = get(),
        )
    }

    // Facades
    single {
        ScopeManagementFacade(
            createScopeHandler = get(),
            updateScopeHandler = get(),
            deleteScopeHandler = get(),
            getScopeByIdHandler = get(),
            getChildrenHandler = get(),
            getRootScopesHandler = get(),
        )
    }
}
