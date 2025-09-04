package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsRequest
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for listing all context views.
 */
class ListContextsCommand :
    CliktCommand(
        name = "list",
        help = """
        List all context views

        Displays all defined context views with their names, keys, and descriptions.
        The currently active context (if any) is highlighted.

        Examples:
            # List all context views
            scopes context list
        """.trimIndent(),
    ),
    KoinComponent {
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val contextOutputFormatter: ContextOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    override fun run() {
        runBlocking {
            when (val result = contextQueryAdapter.listContexts(ListContextViewsRequest)) {
                is ContextViewContract.ListContextViewsResponse.Success -> {
                    if (result.contextViews.isEmpty()) {
                        echo("No context views defined.")
                        echo("Create one with: scopes context create <key> <name> --filter <expression>")
                    } else {
                        // Get current active context to highlight it
                        val currentContextResult = contextQueryAdapter.getCurrentContext(GetActiveContextRequest)
                        val currentContextKey = when (currentContextResult) {
                            is ContextViewContract.GetActiveContextResponse.Success -> currentContextResult.contextView?.key
                            else -> null
                        }
                        echo(contextOutputFormatter.formatContextList(result.contextViews, currentContextKey, debugContext.debug))
                    }
                }
            }
        }
    }
}
