package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.DefineCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.EditCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.ListAspectsCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.ListDefinitionsCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.RemoveAspectCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.RmCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.SetAspectCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.ShowCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.aspect.ValidateCommand

/**
 * Parent command for aspect management operations.
 *
 * This command serves as a group for all aspect-related subcommands,
 * providing a unified interface for managing scope aspects.
 *
 * Note: Subcommands handle errors using CliktError framework.
 */
class AspectCommand :
    CliktCommand(
        name = "aspect",
        help = """
        Manage scope aspects

        Aspects provide key-value metadata for scopes, enabling flexible
        classification and querying capabilities.

        Examples:
            # Define a new aspect
            scopes aspect define priority --type ordered --values "low,medium,high" -d "Task priority level"

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
    // Definition management commands
    DefineCommand(),
    ShowCommand(),
    EditCommand(),
    RmCommand(),
    ListDefinitionsCommand(),
    ValidateCommand(),
    // Scope aspect commands
    SetAspectCommand(),
    ListAspectsCommand(),
    RemoveAspectCommand(),
)
