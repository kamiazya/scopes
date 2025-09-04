package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.formatters.ContextOutputFormatter
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for showing the currently active context.
 */
class CurrentContextCommand :
    CliktCommand(
        name = "current",
        help = """
        Show the currently active context

        Displays information about the currently active context view,
        or indicates that no context is active.

        Examples:
            # Show the current context
            scopes context current

            # Clear the current context
            scopes context current --clear
        """.trimIndent(),
    ),
    KoinComponent {
    private val contextCommandAdapter: ContextCommandAdapter by inject()
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val contextOutputFormatter: ContextOutputFormatter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val clear by option(
        "-c",
        "--clear",
        help = "Clear the current context (show all scopes)",
    ).flag()

    override fun run() {
        runBlocking {
            if (clear) {
                // Clear the current context - this needs clearCurrentContext method in adapter
                echo("Context clearing is not yet implemented.")
                echo("Use 'scopes context switch <key>' to switch to a different context.")
            } else {
                // Show the current context
                when (val result = contextQueryAdapter.getCurrentContext(GetActiveContextRequest)) {
                    is ContextViewContract.GetActiveContextResponse.Success -> {
                        val context = result.contextView
                        if (context == null) {
                            echo("No context is currently active.")
                            echo("All scopes are visible.")
                        } else {
                            echo("Current context: ${context.key}")
                            echo(contextOutputFormatter.formatContextView(context, debugContext.debug))
                        }
                    }
                }
            }
        }
    }
}
