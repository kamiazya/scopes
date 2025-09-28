package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.apps.cli.di.observabilityModule
import io.github.kamiazya.scopes.apps.cli.di.platform.platformModule
import io.github.kamiazya.scopes.interfaces.grpc.client.daemon.di.grpcClientModule
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
import io.github.kamiazya.scopes.interfaces.cli.transport.GrpcTransport
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport
import org.koin.dsl.module

/**
 * Root module for CLI application (gRPC client only)
 *
 * This module includes only what's needed for a gRPC client:
 * - Platform modules (observability, lifecycle)
 * - gRPC client module
 * - CLI interface components
 */
val cliAppModule = module {
    // Core platform modules
    includes(
        observabilityModule,
        platformModule, // Lifecycle management
        grpcClientModule, // gRPC client for daemon communication
    )

    // CLI Commands
    factory { ScopesCommand() }
    factory { CreateCommand() }
    factory { GetCommand() }
    factory { UpdateCommand() }
    factory { DeleteCommand() }
    factory { ListCommand() }
    factory { AliasCommand().configureSubcommands() }

    // CLI Formatters
    single { ScopeOutputFormatter() }
    single { AliasOutputFormatter() }
    single { ContextOutputFormatter() }

    // Always use gRPC transport (direct injection)
    single<Transport> {
        GrpcTransport(
            gatewayClient = get(), // From grpcClientModule
            logger = get(),
        )
    }
}
