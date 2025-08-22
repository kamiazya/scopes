package io.github.kamiazya.scopes.boot.cli

/**
 * Main entry point for the Scopes CLI application.
 * This is a thin entry point that only handles initialization and delegation.
 */
fun main(args: Array<String>) {
    // Delegate to apps layer
    io.github.kamiazya.scopes.apps.cli.SimpleScopesCommand().main(args)
}
