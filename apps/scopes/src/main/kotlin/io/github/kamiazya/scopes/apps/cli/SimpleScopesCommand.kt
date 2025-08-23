package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand

/**
 * Main entry point for the Scopes CLI application.
 *
 * This is a minimal launcher that initializes the DI container
 * and delegates to the ScopesCommand from the interfaces layer.
 */
fun main(args: Array<String>) {
    // Initialize Koin DI
    KoinCompositionRoot.init()

    try {
        // Get the main command from DI and run it
        val command = KoinCompositionRoot.koin.get<ScopesCommand>()
        command.main(args)
    } finally {
        // Clean up resources
        KoinCompositionRoot.close()
    }
}
