package io.github.kamiazya.scopes.interfaces.cli.commands.context

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.commands.DebugContext
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for deleting a context view.
 */
class DeleteContextCommand :
    CliktCommand(
        name = "delete",
        help = """
        Delete a context view

        Removes a context view definition. If the context is currently active,
        it will be deactivated first unless --force is specified.

        Examples:
            # Delete a context view
            scopes context delete my-work

            # Force delete even if it's the active context
            scopes context delete my-work --force
        """.trimIndent(),
    ),
    KoinComponent {
    private val contextCommandAdapter: ContextCommandAdapter by inject()
    private val contextQueryAdapter: ContextQueryAdapter by inject()
    private val debugContext by requireObject<DebugContext>()

    private val key by argument(
        name = "key",
        help = "Context view key to delete",
    )

    private val force by option(
        "-f",
        "--force",
        help = "Force deletion even if context is currently active",
    ).flag()

    override fun run() {
        runBlocking {
            // Check if this is the current context
            val currentContextResult = contextQueryAdapter.getCurrentContext(GetActiveContextRequest)
            if (currentContextResult is ContextViewContract.GetActiveContextResponse.Success) {
                val currentContext = currentContextResult.contextView
                if (currentContext?.key == key && !force) {
                    echo("Error: Cannot delete the currently active context '$key'.", err = true)
                    echo("Use --force to delete it anyway, or switch to a different context first.", err = true)
                    return@runBlocking
                }

                // If forcing deletion of current context, clear it first
                if (currentContext?.key == key && force) {
                    echo("Clearing current context before deletion...")
                }
            }

            val result = contextCommandAdapter.deleteContext(DeleteContextViewRequest(key))
            result.fold(
                { error ->
                    echo("Error: Failed to delete context '$key': ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                },
                {
                    echo("Context view '$key' deleted successfully")
                },
            )
        }
    }
}
