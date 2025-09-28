package io.github.kamiazya.scopes.apps.cli.di.platform

import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationLifecycleManager
import io.github.kamiazya.scopes.platform.commons.time.TimeProvider
import io.github.kamiazya.scopes.platform.infrastructure.lifecycle.DefaultApplicationLifecycleManager
import io.github.kamiazya.scopes.platform.infrastructure.time.SystemTimeProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for platform services.
 *
 * This module provides:
 * - Application lifecycle management
 * - Other platform-level services
 */
val platformModule = module {
    // Time provider for consistent time handling and testability
    single<TimeProvider> {
        SystemTimeProvider()
    }

    // Collect all ApplicationBootstrapper instances for lifecycle management
    single<List<ApplicationBootstrapper>> {
        // gRPC-only CLI doesn't need any bootstrappers (they depend on SQLite infrastructure)
        // All bootstrappers were removed when converting to gRPC-only client
        emptyList()
    }

    // Application lifecycle management
    single<ApplicationLifecycleManager> {
        DefaultApplicationLifecycleManager(logger = get())
    }
}
