package io.github.kamiazya.scopes.interfaces.shared.di

import io.github.kamiazya.scopes.interfaces.shared.facade.ScopeManagementFacade
import org.koin.dsl.module

/**
 * Koin module for shared interface components
 *
 * This module provides:
 * - Shared facades that aggregate multiple bounded contexts
 * - Common interface utilities
 */
val interfaceSharedModule = module {
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
