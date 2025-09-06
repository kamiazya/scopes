package io.github.kamiazya.scopes.apps.cli.di.platform

import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationLifecycleManager
import io.github.kamiazya.scopes.platform.infrastructure.lifecycle.DefaultApplicationLifecycleManager
import org.koin.dsl.module

/**
 * Koin module for platform services.
 *
 * This module provides:
 * - Application lifecycle management
 * - Other platform-level services
 */
val platformModule = module {
    // Application lifecycle management
    single<ApplicationLifecycleManager> {
        DefaultApplicationLifecycleManager(logger = get())
    }
}
