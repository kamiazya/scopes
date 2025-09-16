package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for editing an existing context view.
 */
class EditContextCommand :
    CliktCommand(
        name = "edit",
        help = """
        Edit an existing context view

        Update the properties of a context view. Only specified options will be changed;
        unspecified properties remain unchanged.

        Examples:
            # Update the name of a context
            scopes context edit my-work --name "My Active Tasks"

            # Update the filter expression
            scopes context edit my-work --filter "assignee=me AND status=active"

            # Update both name and description
            scopes context edit urgent --name "Critical Items" --description "High priority tasks requiring immediate attention"
        """.trimIndent(),
    ),
    KoinComponent {
    private val contextCommandAdapter: ContextCommandAdapter by inject()
    private val contextOutputFormatter: ContextOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val key by argument(
        name = "key",
        help = "Context view key to edit",
    )

    private val name by option(
        "-n",
        "--name",
        help = "New human-readable name for the context view",
    )

    private val filter by option(
        "-f",
        "--filter",
        help = "New aspect query filter expression",
    )

    private val description by option(
        "-d",
        "--description",
        help = "New description (use empty string to clear)",
    )

    override fun run() {
        runBlocking {
            // Check if at least one field to update is provided
            if (name == null && filter == null && description == null) {
                echo("Error: At least one field to update must be specified (--name, --filter, or --description)", err = true)
                return@runBlocking
            }

            val request = UpdateContextViewCommand(
                key = key,
                name = name,
                filter = filter,
                description = description,
            )

            val result = contextCommandAdapter.updateContext(request)
            result.fold(
                { error ->
                    echo("Error: Failed to update context '$key': ${ErrorMessageMapper.toUserMessage(error)}", err = true)
                },
                {
                    echo("Context view '$key' updated successfully")
                },
            )
        }
    }
}
