package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.apps.cli.di.cliAppModule
import io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap.AspectPresetBootstrap
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Koin-based Application container for the CLI
 *
 * This manages the DI container lifecycle and provides
 * access to dependencies.
 */
class ScopesCliApplication : AutoCloseable {
    private var koinApp: KoinApplication = startKoin {
        // Set environment property for database configuration
        properties(
            mapOf(
                "app.environment" to (System.getenv("APP_ENVIRONMENT") ?: "development"),
            ),
        )
        // Load modules
        modules(cliAppModule)
    }

    private var isInitialized = false

    /**
     * Initialize aspect presets lazily.
     * This will be called automatically when needed.
     */
    suspend fun ensureInitialized() {
        if (!isInitialized) {
            val aspectPresetBootstrap = koinApp.koin.get<AspectPresetBootstrap>()
            aspectPresetBootstrap.initialize().fold(
                ifLeft = { error ->
                    // Log error but don't fail application startup
                    println("Warning: Failed to initialize aspect presets: $error")
                },
                ifRight = {
                    // Success - presets initialized
                },
            )
            isInitialized = true
        }
    }

    /**
     * Clean up Koin resources
     */
    override fun close() {
        stopKoin()
    }

    // Direct access to Koin instance
    val container: Koin
        get() = koinApp.koin
}
