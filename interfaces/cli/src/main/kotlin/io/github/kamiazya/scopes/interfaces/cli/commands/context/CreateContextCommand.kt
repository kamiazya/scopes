package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for creating a new context view.
 */
class CreateContextCommand :
    CliktCommand(
        name = "create",
    ),
    KoinComponent {
    override fun help(context: com.github.ajalt.clikt.core.Context) = """
        Create a new context view

        Context views are named filter configurations that can be reused across sessions.
        They help organize and quickly access different work contexts.

        Examples:
            # Create a context for active tasks assigned to you
            scopes context create my-work "My Work" --filter "assignee=me AND status!=closed" -d "Active tasks assigned to me"

            # Create a context for client project work
            scopes context create client-a "Client A Project" --filter "project=client-a" -d "All work for Client A"

            # Create a context for urgent items
            scopes context create urgent "Urgent Items" --filter "priority=high OR priority=critical" -d "High priority items"
    """.trimIndent()
    private val contextCommandAdapter: ContextCommandAdapter by inject()
    private val contextOutputFormatter: ContextOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val key by argument(
        name = "key",
        help = "Unique identifier for the context view (e.g., 'my-work', 'client-project')",
    )

    private val name by argument(
        name = "name",
        help = "Human-readable name for the context view",
    )

    private val filter by option(
        "-f",
        "--filter",
        help = "Aspect query filter expression (e.g., 'status=active AND assignee=me')",
    ).required()

    private val description by option(
        "-d",
        "--description",
        help = "Optional description of what this context view represents",
    )

    override fun run() {
        runBlocking {
            val request = CreateContextViewCommand(
                key = key,
                name = name,
                filter = filter,
                description = description,
            )

            val result = contextCommandAdapter.createContext(request)
            result.fold(
                ifLeft = { error ->
                    throw CliktError("Failed to create context '$key': ${ErrorMessageMapper.getMessage(error)}")
                },
                ifRight = {
                    echo("Context view '$key' created successfully")
                },
            )
        }
    }
}
