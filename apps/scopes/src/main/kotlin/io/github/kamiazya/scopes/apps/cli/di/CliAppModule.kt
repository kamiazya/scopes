package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementInfrastructureModule
import io.github.kamiazya.scopes.apps.cli.di.scopemanagement.scopeManagementModule
import io.github.kamiazya.scopes.interfaces.cli.di.interfaceCliModule
import io.github.kamiazya.scopes.interfaces.shared.di.interfaceSharedModule
import io.github.kamiazya.scopes.platform.observability.di.observabilityModule
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

        // Interface layers
        interfaceSharedModule,
        interfaceCliModule,
    )
}
