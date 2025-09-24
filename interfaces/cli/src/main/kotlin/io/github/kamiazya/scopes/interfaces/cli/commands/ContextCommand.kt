package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.kamiazya.scopes.interfaces.cli.commands.context.CreateContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.CurrentContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.DeleteContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.EditContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.ListContextsCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.ShowContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.context.SwitchContextCommand

/**
 * Parent command for context view management operations.
 *
 * This command serves as a group for all context-related subcommands,
 * providing a unified interface for managing context views.
 *
 * Note: Subcommands handle errors using CliktError framework.
 */
class ContextCommand :
    CliktCommand(
        name = "context",
    ) {
    override val printHelpOnEmptyArgs = true
    override fun help(context: com.github.ajalt.clikt.core.Context) = """
        Manage context views

        Context views provide named, persistent filter configurations for scopes,
        enabling quick switching between different work contexts.

        Examples:
            # Create a new context view
            scopes context create my-work "My Work" --filter "assignee=me AND status!=closed" -d "Active tasks assigned to me"

            # List all context views
            scopes context list

            # Show details of a specific context
            scopes context show my-work

            # Edit an existing context
            scopes context edit my-work --name "My Active Work"

            # Delete a context view
            scopes context delete my-work
    """.trimIndent()

    override fun run() {
        // This is a parent command that does nothing on its own
        // Subcommands handle the actual functionality
    }
}

/**
 * Extension function to configure context subcommands
 */
fun ContextCommand.configureSubcommands(): ContextCommand {
    subcommands(
        CreateContextCommand(),
        ListContextsCommand(),
        ShowContextCommand(),
        EditContextCommand(),
        DeleteContextCommand(),
        SwitchContextCommand(),
        CurrentContextCommand(),
    )
    return this
}
