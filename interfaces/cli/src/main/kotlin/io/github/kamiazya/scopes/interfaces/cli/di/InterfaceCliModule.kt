package io.github.kamiazya.scopes.interfaces.cli.di

import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.CreateCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.DeleteCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.GetCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ListCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.UpdateCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import org.koin.dsl.module

/**
 * Koin module for CLI interface components
 *
 * This module provides:
 * - CLI commands
 * - CLI-specific adapters
 * - Output formatters
 * - CLI utilities
 */
val interfaceCliModule = module {
    // Commands
    factory { ScopesCommand() }
    factory { CreateCommand() }
    factory { GetCommand() }
    factory { UpdateCommand() }
    factory { DeleteCommand() }
    factory { ListCommand() }

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
