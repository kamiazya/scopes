package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Update command for modifying existing scopes.
 */
class UpdateCommand :
    CliktCommand(
        name = "update",
        help = "Update an existing scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()

    private val id by argument(help = "ID of the scope to update")
    private val title by option("-t", "--title", help = "New title for the scope")
    private val description by option("-d", "--description", help = "New description for the scope")

    override fun run() {
        runBlocking {
            scopeCommandAdapter.updateScope(
                id = id,
                title = title,
                description = description,
            ).fold(
                { error ->
                    echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                },
                { scope ->
                    echo(scopeOutputFormatter.formatContractUpdateResult(scope))
                },
            )
        }
    }
}
