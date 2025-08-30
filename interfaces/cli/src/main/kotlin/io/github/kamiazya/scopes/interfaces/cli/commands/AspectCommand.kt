package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.ListAspectsCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.RemoveAspectCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.SetAspectCommand

/**
 * Parent command for aspect management operations.
 *
 * This command serves as a group for all aspect-related subcommands,
 * providing a unified interface for managing scope aspects.
 */
class AspectCommand :
    CliktCommand(
        name = "aspect",
        help = """
        Manage scope aspects

        Aspects provide key-value metadata for scopes, enabling flexible
        classification and querying capabilities.

        Examples:
          # Set an aspect on a scope
          scopes aspect set quiet-river-a4f7 priority=high

          # List all aspects for a scope
          scopes aspect list quiet-river-a4f7

          # Remove an aspect from a scope
          scopes aspect remove quiet-river-a4f7 priority
        """.trimIndent(),
        printHelpOnEmptyArgs = true,
    ) {
    override fun run() {
        // This is a parent command that does nothing on its own
        // Subcommands handle the actual functionality
    }
}

/**
 * Extension function to configure aspect subcommands
 */
fun AspectCommand.configureSubcommands(): AspectCommand = subcommands(
    SetAspectCommand(),
    ListAspectsCommand(),
    RemoveAspectCommand(),
)
