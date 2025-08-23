package io.github.kamiazya.scopes.platform.observability.di

import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for observability platform components
 *
 * This module provides:
 * - Logging infrastructure
 * - Monitoring utilities
 * - Telemetry components
 */
val observabilityModule: Module = module {
    single<Logger> { ConsoleLogger() }
}
