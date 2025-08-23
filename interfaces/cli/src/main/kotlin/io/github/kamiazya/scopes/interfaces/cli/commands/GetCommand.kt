package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Get command for retrieving scopes.
 */
class GetCommand :
    CliktCommand(
        name = "get",
        help = "Get a scope by ID",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()

    private val id by argument(help = "ID of the scope to retrieve")

    override fun run() {
        runBlocking {
            scopeCommandAdapter.getScopeById(id).fold(
                { error ->
                    echo("Error: ${ErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo(scopeOutputFormatter.formatScope(scope))
                },
            )
        }
    }
}
