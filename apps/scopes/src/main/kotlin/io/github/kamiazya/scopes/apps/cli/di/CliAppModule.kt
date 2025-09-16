package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.apps.cli.di.contractsModule
import io.github.kamiazya.scopes.apps.cli.di.devicesync.deviceSyncInfrastructureModule
import io.github.kamiazya.scopes.apps.cli.di.eventstore.eventStoreInfrastructureModule
import io.github.kamiazya.scopes.apps.cli.di.observabilityModule
import io.github.kamiazya.scopes.apps.cli.di.platform.databaseModule
import io.github.kamiazya.scopes.apps.cli.di.platform.platformModule
import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementInfrastructureModule
import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementModule
import io.github.kamiazya.scopes.interfaces.mcp.di.simpleMcpModule
import io.github.kamiazya.scopes.apps.cli.di.userpreferences.userPreferencesModule
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.AliasCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.CreateCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.DeleteCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.GetCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ListCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.UpdateCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.configureSubcommands
import io.github.kamiazya.scopes.interfaces.cli.formatters.AliasOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
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
        databaseModule,
        platformModule, // Lifecycle management

        // Bounded Contexts
        scopeManagementModule,
        scopeManagementInfrastructureModule,
        eventStoreInfrastructureModule,
        deviceSyncInfrastructureModule,
        userPreferencesModule,

        // Contracts layer
        contractsModule,

        // MCP components
        simpleMcpModule,
    )

    // CLI Commands
    factory { ScopesCommand() }
    factory { CreateCommand() }
    factory { GetCommand() }
    factory { UpdateCommand() }
    factory { DeleteCommand() }
    factory { ListCommand() }
    factory { AliasCommand().configureSubcommands() }

    // CLI Adapters
    single {
        ScopeCommandAdapter(
            scopeManagementCommandPort = get(),
            // Future: Add other context ports here
        )
    }
    single {
        AliasCommandAdapter(
            scopeManagementCommandPort = get(),
        )
    }
    single {
        AliasQueryAdapter(
            scopeManagementQueryPort = get(),
        )
    }
    single {
        io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter(
            scopeManagementQueryPort = get(),
        )
    }
    single {
        AspectCommandAdapter(
            aspectCommandPort = get(),
        )
    }
    single {
        ContextCommandAdapter(
            contextViewCommandPort = get(),
        )
    }
    single {
        ContextQueryAdapter(
            contextViewQueryPort = get(),
        )
    }
    single {
        AspectQueryAdapter(
            aspectQueryPort = get(),
        )
    }

    // CLI Formatters
    single { ScopeOutputFormatter() }
    single { AliasOutputFormatter() }
    single { ContextOutputFormatter() }

    // CLI Utilities
    single {
        ScopeParameterResolver(
            scopeManagementPort = get(),
        )
    }

    // MCP server provided by simpleMcpModule
}
