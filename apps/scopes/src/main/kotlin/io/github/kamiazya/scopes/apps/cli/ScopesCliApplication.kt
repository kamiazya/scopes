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
        // Load modules
        modules(cliAppModule)
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
