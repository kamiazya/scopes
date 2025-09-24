package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for switching to a different context view.
 */
class SwitchContextCommand :
    CliktCommand(
        name = "switch",
    ),
    KoinComponent {
    override fun help(context: com.github.ajalt.clikt.core.Context) = """
        Switch to a different context view

        Sets the specified context view as the current active context.
        This affects which scopes are visible when using list commands.

        Examples:
            # Switch to the 'my-work' context
            scopes context switch my-work

            # Switch to a different project context
            scopes context switch client-project

            # Clear the current context (show all scopes)
            scopes context switch --clear
    """.trimIndent()
    private val contextCommandAdapter: ContextCommandAdapter by inject()
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val key by argument(
        name = "key",
        help = "Context view key to switch to",
    )

    override fun run() {
        runBlocking {
            val result = contextCommandAdapter.setCurrentContext(SetActiveContextCommand(key))
            result.fold(
                { error ->
                    echo("Error: Failed to switch to context '$key': ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                {
                    echo("Switched to context '$key'")

                    // Show the active filter
                    contextQueryAdapter.getContextView(key).fold(
                        { /* Ignore errors - context was switched successfully but details not available */ },
                        { contextView ->
                            contextView?.let {
                                echo("Active filter: ${it.filter}")
                            }
                        },
                    )
                },
            )
        }
    }
}
