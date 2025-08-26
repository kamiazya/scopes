package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementInfrastructureModule
import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementModule
import io.github.kamiazya.scopes.apps.cli.di.userpreferences.userPreferencesModule
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.CreateCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.DeleteCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.GetCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ListCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.UpdateCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import org.koin.dsl.module

/**
 * Root module for CLI application
 *
 * This module aggregates all necessary modules for the CLI application:
 * - Platform modules
 * - Bounded context modules
 * - Interface modules
 * - Application-specific components
 */
val cliAppModule = module {
    // Include all required modules
    includes(
        // Platform
        observabilityModule,

        // Bounded Contexts
        scopeManagementModule,
        scopeManagementInfrastructureModule,
        userPreferencesModule,

        // Contracts layer
        contractsModule,
    )

    // CLI Commands
    factory { ScopesCommand() }
    factory { CreateCommand() }
    factory { GetCommand() }
    factory { UpdateCommand() }
    factory { DeleteCommand() }
    factory { ListCommand() }

    // CLI Adapters
    single {
        ScopeCommandAdapter(
            scopeManagementPort = get(),
            // Future: Add other context ports here
        )
    }

    // CLI Formatters
    single { ScopeOutputFormatter() }

    // CLI Utilities
    single {
        ScopeParameterResolver(
            scopeManagementPort = get(),
        )
    }
}
