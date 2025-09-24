package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.interfaces.cli.commands.alias.AddAliasCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.alias.ListAliasesCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.alias.RemoveAliasCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.alias.RenameAliasCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.alias.SetCanonicalAliasCommand

/**
 * Parent command for alias management operations.
 *
 * This command serves as a group for all alias-related subcommands,
 * providing a unified interface for managing scope aliases.
 *
 * Note: Subcommands handle errors using CliktError framework.
 */
class AliasCommand :
    CliktCommand(
        name = "alias",
    ) {
    override val printHelpOnEmptyArgs = true
    override fun help(context: com.github.ajalt.clikt.core.Context) = """
        Manage scope aliases

        Aliases provide user-friendly names for scopes instead of using ULIDs.
        Each scope can have multiple aliases, with one designated as canonical.

        Examples:
            # Add a new alias to a scope
            scopes alias add "project-alpha" --scope 01HX5Y2Z1A2B3C4D5E6F7G8H9J

            # List all aliases for a scope
            scopes alias list --scope project-alpha

            # Set the canonical alias for a scope
            scopes alias set-canonical "alpha" --scope project-alpha

            # Remove an alias
            scopes alias rm "old-name" --scope project-alpha

            # Rename an alias
            scopes alias rename "old-name" "new-name"
    """.trimIndent()

    override fun run() {
        // This is a parent command that does nothing on its own
        // Subcommands handle the actual functionality
    }
}

/**
 * Extension function to configure alias subcommands
 */
fun AliasCommand.configureSubcommands(): AliasCommand = subcommands(
    AddAliasCommand(),
    ListAliasesCommand(),
    SetCanonicalAliasCommand(),
    RemoveAliasCommand(),
    RenameAliasCommand(),
)
