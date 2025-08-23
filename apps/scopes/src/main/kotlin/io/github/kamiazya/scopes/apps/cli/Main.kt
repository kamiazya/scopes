package io.github.kamiazya.scopes.apps.cli

import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand

/**
 * Main entry point for the Scopes CLI application.
 *
 * This is a minimal launcher that initializes the DI container
 * and delegates to the ScopesCommand from the interfaces layer.
 */
fun main(args: Array<String>) = ScopesCliApplication()
    .use {
        it.container.get<ScopesCommand>().main(args)
    }
