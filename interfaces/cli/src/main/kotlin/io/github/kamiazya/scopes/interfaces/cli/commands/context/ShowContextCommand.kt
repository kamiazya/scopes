package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewRequest
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for showing details of a specific context view.
 *
 * Note: Uses CliktError for error handling.
 */
class ShowContextCommand :
    CliktCommand(
        name = "show",
        help = """
        Show details of a specific context view

        Displays the full configuration and metadata of a context view,
        including its filter expression and timestamps.

        Examples:
            # Show details of the 'my-work' context
            scopes context show my-work

            # Show details of the current context
            scopes context show current
        """.trimIndent(),
    ),
    KoinComponent {
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val contextOutputFormatter: ContextOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val key by argument(
        name = "key",
        help = "Context view key to show (use 'current' for the active context)",
    )

    override fun run() {
        runBlocking {
            val contextKey = if (key == "current") {
                // Get the current context key
                when (val result = contextQueryAdapter.getCurrentContext(GetActiveContextRequest)) {
                    is ContextViewContract.GetActiveContextResponse.Success -> {
                        val activeContext = result.contextView
                        if (activeContext == null) {
                            echo("No context is currently active.", err = true)
                            return@runBlocking
                        }
                        activeContext.key
                    }
                    else -> {
                        echo("Failed to get current context.", err = true)
                        return@runBlocking
                    }
                }
            } else {
                key
            }

            when (val result = contextQueryAdapter.getContext(GetContextViewRequest(contextKey))) {
                is ContextViewContract.GetContextViewResponse.Success -> {
                    echo(contextOutputFormatter.formatContextViewDetailed(result.contextView, debugContext.debug))
                }
                is ContextViewContract.GetContextViewResponse.NotFound -> {
                    echo("Context view '${result.key}' not found.", err = true)
                }
            }
        }
    }
}
