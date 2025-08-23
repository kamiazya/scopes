package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.apps.cli.di.cliAppModule
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

    // Direct access to Koin instance
    val koin: org.koin.core.Koin
        get() = koinApp.koin
}
