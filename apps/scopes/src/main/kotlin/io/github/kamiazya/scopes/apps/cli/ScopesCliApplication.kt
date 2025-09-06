package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.apps.cli.di.cliAppModule
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationLifecycleManager
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

    /**
     * Initialize application using the lifecycle manager.
     * This will initialize all registered ApplicationBootstrapper components.
     */
    suspend fun initialize() {
        val lifecycleManager = koinApp.koin.get<ApplicationLifecycleManager>()
        val errors = lifecycleManager.initialize()

        if (errors.isNotEmpty()) {
            // Log initialization errors but don't fail application startup
            errors.forEach { error ->
                val critical = if (error.isCritical) " [CRITICAL]" else ""
                println("Warning: Bootstrap error in ${error.component}$critical: ${error.message}")
            }
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
