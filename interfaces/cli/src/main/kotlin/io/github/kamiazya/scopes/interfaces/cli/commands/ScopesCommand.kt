package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * Main CLI command for Scopes application.
 *
 * This is the root command that serves as the entry point for all
 * scope-related CLI operations. It delegates to subcommands for
 * specific operations.
 */
class ScopesCommand :
    CliktCommand(
        name = "scopes",
        help = "Scopes - AI-Native Task Management System",
    ) {
    init {
        subcommands(
            CreateCommand(),
            GetCommand(),
            UpdateCommand(),
            DeleteCommand(),
            ListCommand(),
            AliasCommand().configureSubcommands(),
        )
    }

    override fun run() {
        // Nothing to do here - subcommands will be executed
    }
}
