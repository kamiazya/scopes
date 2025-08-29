package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

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

    private val debug by option("--debug", help = "Enable debug output showing ULIDs alongside aliases").flag()

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
        // Store debug flag in context for subcommands to access
        currentContext.obj = DebugContext(debug)
    }
}
