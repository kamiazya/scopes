package io.github.kamiazya.scopes.interfaces.cli.di

import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import org.koin.dsl.module

/**
 * Koin module for CLI interface components
 *
 * This module provides:
 * - CLI-specific adapters
 * - Output formatters
 * - CLI utilities
 */
val interfaceCliModule = module {
    // Adapters
    single {
        ScopeCommandAdapter(
            scopeManagementFacade = get(),
            // Future: Add other context facades here
        )
    }

    // Formatters
    single { ScopeOutputFormatter() }
}
