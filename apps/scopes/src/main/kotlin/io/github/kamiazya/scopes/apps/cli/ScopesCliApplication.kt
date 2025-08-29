package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.apps.cli.di.cliAppModule
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

    init {
        // TODO: Re-enable after complete migration to SQLDelight
        // Currently disabled as we're migrating from Exposed to SQLDelight
        // val databaseBootstrap = koinApp.koin.get<DatabaseBootstrap>()
        // databaseBootstrap.initialize()
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
