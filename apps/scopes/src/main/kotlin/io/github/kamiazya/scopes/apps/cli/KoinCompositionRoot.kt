package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.apps.cli.di.cliAppModule
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Koin-based Composition Root for the CLI application
 *
 * This replaces the manual dependency injection with Koin's
 * declarative dependency injection framework.
 */
object KoinCompositionRoot {
    private lateinit var koinApp: KoinApplication

    /**
     * Initialize Koin with all required modules
     */
    fun init() {
        if (::koinApp.isInitialized) {
            // Koin is already initialized
            return
        }

        koinApp = startKoin {
            // Load modules
            modules(cliAppModule)
        }
    }

    /**
     * Clean up Koin resources
     */
    fun close() {
        if (::koinApp.isInitialized) {
            stopKoin()
        }
    }

    // Convenience accessors for CLI components
    val scopeCommandAdapter: ScopeCommandAdapter
        get() = koinApp.koin.get()

    val scopeOutputFormatter: ScopeOutputFormatter
        get() = koinApp.koin.get()

    // Direct access to Koin instance for testing
    val koin
        get() = koinApp.koin
}
